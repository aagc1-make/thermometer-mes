package com.ktg.mes.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.LinkOption;
import java.util.List;

@Service
public class MesKnowledgeUploadService {
    private static final Logger log = LoggerFactory.getLogger(MesKnowledgeUploadService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final MesKnowledgeIndexer indexer;
    private final Path storageDirectory;
    private final MesKnowledgeTextExtractor extractor;
    private static final String STORAGE_KEY_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.("
                    + MesKnowledgeTextExtractor.SUPPORTED_EXTENSIONS + ")";
    private final MesKnowledgeGraphIndexer graphIndexer;

    public MesKnowledgeUploadService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                                     MesKnowledgeIndexer indexer,
                                     @Value("${mes.knowledge.storage-directory:./runtime/mes-knowledge}")
                                     String storageDirectory,MesKnowledgeTextExtractor extractor,
                                     MesKnowledgeGraphIndexer graphIndexer) {
        this.jdbc = jdbc;
        this.indexer = indexer;
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.extractor = extractor;
        this.graphIndexer = graphIndexer;
    }

    public Map<String, Object> upload(long ownerId, String title, MultipartFile file)
            throws IOException {
        return save(ownerId, null, title, file);
    }

    public Map<String, Object> uploadVersion(long ownerId, long documentId, MultipartFile file)
            throws IOException {
        return save(ownerId, documentId, null, file);
    }

    private Map<String, Object> save(long ownerId, Long existingDocumentId,
                                     String title, MultipartFile file) throws IOException {
        String cleanTitle = title == null ? "" : title.trim();
        if (existingDocumentId == null && (cleanTitle.isEmpty() || cleanTitle.length() > 200)) {
            throw new IllegalArgumentException("文档标题须为 1 至 200 个字符");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        originalName = originalName.replace('\\', '/');
        originalName = originalName.substring(originalName.lastIndexOf('/') + 1);

        if (originalName.length() > 255
                || originalName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("文件名过长或包含无效字符");
        }

        String extension = MesKnowledgeTextExtractor.supportedExtension(originalName);
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("请上传非空且不超过 5 MiB 的文件");
        }

        byte[] bytes = file.getBytes();
        List<MesKnowledgeTextExtractor.Section> sections;
        try {
            sections = extractor.extract(originalName, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("无法解析文件，请确认文件格式正确且未损坏", e);
        }
        List<MesKnowledgeChunks.Chunk> fragments = MesKnowledgeChunks.split(sections);
        String storageKey = UUID.randomUUID() + "." + extension;
        Files.createDirectories(storageDirectory);
        Path savedFile = storageDirectory.resolve(storageKey);
        try (java.io.OutputStream output = Files.newOutputStream(savedFile, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            output.write(bytes);
        }

        Long[] ids;
        String displayName = originalName;
        try {
            ids = transaction.execute(status -> existingDocumentId == null
                    ? insertPending(ownerId, cleanTitle, displayName, storageKey, bytes.length)
                    : insertPendingVersion(ownerId, existingDocumentId,
                    displayName, storageKey, bytes.length));
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(savedFile);
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }

        long documentId = ids[0];
        long versionId = ids[1];
        Long previousVersionId = ids[2];
        String indexStatus = "READY";
        try {
            indexer.index(ownerId, documentId, versionId, fragments);
            graphIndexer.index(ownerId, documentId, versionId, fragments);

            transaction.executeWithoutResult(status ->
                    activate(ownerId, documentId, versionId, previousVersionId));
        } catch (RuntimeException e) {
            log.warn("知识库索引或版本激活失败，文档 ID {}，版本 ID {}，异常类型 {}",
                    documentId, versionId, e.getClass().getSimpleName());
            transaction.executeWithoutResult(status -> {
                int updated = jdbc.update("UPDATE mes_kb_version SET index_status = 'FAILED' "
                                + "WHERE version_id = ? AND document_id = ? AND index_status = 'PENDING'",
                        versionId, documentId);
                requireOneRow(updated);
            });
            indexStatus = "FAILED";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId);
        result.put("versionId", versionId);
        result.put("indexStatus", indexStatus);
        return result;
    }

    private Long[] insertPending(long ownerId, String title, String originalName, String storageKey, long size) {
        KeyHolder documentKey = new GeneratedKeyHolder();
        requireOneRow(jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO mes_kb_document (owner_id, title) VALUES (?, ?)", new String[]{"document_id"});
            statement.setLong(1, ownerId);
            statement.setString(2, title);
            return statement;
        }, documentKey));
        long documentId = generatedId(documentKey);

        long versionId = insertVersion(documentId, 1, originalName, storageKey, size);
        return new Long[]{documentId, versionId, null};
    }

    private void activate(long ownerId, long documentId, long versionId, Long previousVersionId) {
        requireOneRow(jdbc.update(
                "UPDATE mes_kb_document SET active_version_id = ? "
                        + "WHERE document_id = ? AND owner_id = ? AND deleted = 0 "
                        + "AND active_version_id <=> ?",
                versionId, documentId, ownerId, previousVersionId));

        requireOneRow(jdbc.update(
                "UPDATE mes_kb_version SET index_status = 'READY' "
                        + "WHERE version_id = ? AND document_id = ? AND index_status = 'PENDING'",
                versionId, documentId));
    }

    private long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("未取得数据库生成的 ID");
        }
        return key.longValue();
    }

    private void requireOneRow(int updated) {
        if (updated != 1) {
            throw new IllegalStateException("知识库记录状态更新失败");
        }
    }

    public ResponseEntity<Resource> download(long ownerId, long documentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT v.storage_key, v.original_name "
                        + "FROM mes_kb_document d "
                        + "JOIN mes_kb_version v ON v.version_id = d.active_version_id "
                        + "AND v.document_id = d.document_id "
                        + "WHERE d.document_id = ? AND d.owner_id = ? "
                        + "AND d.deleted = 0 AND v.index_status = 'READY'",
                documentId, ownerId
        );

        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> row = rows.get(0);
        String storageKey = (String) row.get("storage_key");
        String originalName = (String) row.get("original_name");

        // 上传时生成的是 UUID 文件名，只接受该格式。
        if (storageKey == null || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            return ResponseEntity.notFound().build();
        }

        Path file = storageDirectory.resolve(storageKey);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String disposition = ContentDisposition.attachment()
                .filename(originalName, StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    public Map<String, Object> rebuildGraph(long ownerId, long documentId)
            throws IOException {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT v.version_id, v.storage_key, v.original_name "
                        + "FROM mes_kb_document d "
                        + "JOIN mes_kb_version v ON v.version_id = d.active_version_id "
                        + "AND v.document_id = d.document_id "
                        + "WHERE d.document_id = ? AND d.owner_id = ? "
                        + "AND d.deleted = 0 AND v.index_status = 'READY'",
                documentId, ownerId);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("文档不存在或没有可重建的生效版本");
        }

        Map<String, Object> version = rows.get(0);
        long versionId = ((Number) version.get("version_id")).longValue();
        String storageKey = (String) version.get("storage_key");
        String originalName = (String) version.get("original_name");

        if (storageKey == null || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            throw new IllegalStateException("原文件存储键无效");
        }

        Path file = storageDirectory.resolve(storageKey);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(file)) {
            throw new IOException("原文件不存在或不可读");
        }

        List<MesKnowledgeTextExtractor.Section> sections =
                extractor.extract(originalName, Files.readAllBytes(file));
        List<MesKnowledgeChunks.Chunk> fragments = MesKnowledgeChunks.split(sections);

        Map<String, Object> graph = graphIndexer.prepareGraph(ownerId, documentId, versionId, fragments);
        transaction.executeWithoutResult(status -> {
            // 模型抽取在锁外完成，写入前重新确认版本，避免重建与删除并发。
            List<Long> active = jdbc.queryForList(
                    "SELECT active_version_id FROM mes_kb_document WHERE document_id = ? AND owner_id = ? "
                            + "AND deleted = 0 FOR UPDATE", Long.class, documentId, ownerId);
            if (active.isEmpty() || !Long.valueOf(versionId).equals(active.get(0))) {
                throw new IllegalArgumentException("文档已删除或生效版本已更新，请刷新列表");
            }
            graphIndexer.writeGraph(graph);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId);
        result.put("versionId", versionId);
        result.put("chunkCount", fragments.size());
        return result;
    }

    private Long[] insertPendingVersion(long ownerId, long documentId,
                                        String originalName, String storageKey, long size) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT active_version_id FROM mes_kb_document "
                        + "WHERE document_id = ? AND owner_id = ? AND deleted = 0 FOR UPDATE",
                documentId, ownerId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("文档不存在或不可更新");
        }

        Number active = (Number) rows.get(0).get("active_version_id");
        Long previousVersionId = active == null ? null : active.longValue();

        Integer nextVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) + 1 FROM mes_kb_version WHERE document_id = ?",
                Integer.class, documentId);

        long versionId = insertVersion(documentId, nextVersion, originalName, storageKey, size);
        return new Long[]{documentId, versionId, previousVersionId};
    }

    private long insertVersion(long documentId, int versionNo, String originalName,
                               String storageKey, long size) {
        KeyHolder versionKey = new GeneratedKeyHolder();
        requireOneRow(jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO mes_kb_version "
                            + "(document_id, version_no, original_name, storage_key, size_bytes, index_status) "
                            + "VALUES (?, ?, ?, ?, ?, 'PENDING')",
                    new String[]{"version_id"});
            statement.setLong(1, documentId);
            statement.setInt(2, versionNo);
            statement.setString(3, originalName);
            statement.setString(4, storageKey);
            statement.setLong(5, size);
            return statement;
        }, versionKey));
        return generatedId(versionKey);
    }

    public boolean deleteDocument(long ownerId, long documentId) {
        List<String> storageKeys = transaction.execute(status -> {
            List<Integer> rows = jdbc.queryForList(
                    "SELECT deleted FROM mes_kb_document "
                            + "WHERE document_id = ? AND owner_id = ? FOR UPDATE",
                    Integer.class, documentId, ownerId);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("文档不存在或不可删除");
            }

            if (rows.get(0) == 0) {
                Integer pending = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM mes_kb_version "
                                + "WHERE document_id = ? AND index_status = 'PENDING'",
                        Integer.class, documentId);
                if (pending != null && pending > 0) {
                    throw new IllegalArgumentException("文档正在索引，请稍后重试");
                }

                requireOneRow(jdbc.update(
                        "UPDATE mes_kb_document SET deleted = 2, active_version_id = NULL "
                                + "WHERE document_id = ? AND owner_id = ? AND deleted = 0",
                        documentId, ownerId));
            }

            return jdbc.queryForList(
                    "SELECT storage_key FROM mes_kb_version WHERE document_id = ?",
                    String.class, documentId);
        });

        try {
            indexer.deleteDocument(ownerId, documentId);
            graphIndexer.deleteDocument(ownerId, documentId);
            for (String key : storageKeys) {
                if (key == null || !key.matches(STORAGE_KEY_PATTERN)) {
                    throw new IllegalStateException("原文件存储键无效");
                }
                Files.deleteIfExists(storageDirectory.resolve(key));
            }
            jdbc.update("UPDATE mes_kb_document SET deleted = 1 WHERE document_id = ? AND owner_id = ? AND deleted = 2",
                    documentId, ownerId);
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("知识库文档清理失败，文档 ID {}", documentId, e);
            return false;
        }
    }
}
