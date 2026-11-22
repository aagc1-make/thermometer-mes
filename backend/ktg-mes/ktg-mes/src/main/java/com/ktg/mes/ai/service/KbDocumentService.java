package com.ktg.mes.ai.service;

import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.SecurityUtils;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.KbDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KbDocumentService {
    private static final Logger log = LoggerFactory.getLogger(KbDocumentService.class);
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final int MAX_DOCUMENTS = 500;
    private static final int PREVIEW_PAGE_SIZE = 20;
    private static final Set<String> EXTENSIONS = new HashSet<>(Arrays.asList("txt", "md", "csv", "pdf", "docx", "xlsx", "html"));
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList("检验规范", "BOM 说明", "工艺说明", "操作手册", "其他"));
    private final KbDocumentMapper mapper;
    private final KbOriginalStorage storage;
    private final PermissionService permissions;

    public KbDocumentService(KbDocumentMapper mapper, KbOriginalStorage storage, PermissionService permissions) {
        this.mapper = mapper;
        this.storage = storage;
        this.permissions = permissions;
    }

    public KbDocument.Catalog list(long libraryId) {
        KbDocument.Library scope = scope(libraryId, false);
        return new KbDocument.Catalog(scope, mapper.listDocuments(scope), mapper.countCharacters(scope),
            MAX_DOCUMENTS, KbDocumentParser.CHARACTER_LIMIT);
    }

    public KbDocument.Library readScope(long libraryId) {
        return scope(libraryId, false);
    }

    @Transactional
    public void upload(long libraryId, Long docId, String title, String category, String versionLabel,
                       boolean graphEnabled, MultipartFile file) {
        KbDocument.Library scope = scope(libraryId, true);
        String label = text(versionLabel, 40, "版本号");
        String filename = filename(file);

        KbDocument document;
        if (docId != null) {
            document = document(scope, docId);
        } else {
            String name = text(title, 120, "文档名称");
            if (!CATEGORIES.contains(category)) {
                throw new ServiceException("请选择有效的文档分类", HttpStatus.BAD_REQUEST);
            }
            document = mapper.findByName(scope, name, category);
            if (document == null) {
                if (mapper.countDocuments(scope) >= MAX_DOCUMENTS) {
                    throw new ServiceException("该知识库已达到 500 份文档上限", HttpStatus.BAD_REQUEST);
                }
                document = new KbDocument();
                document.setLibraryId(scope.getLibraryId());
                document.setDeptId(scope.getDeptId());
                document.setTitle(name);
                document.setCategory(category);
                document.setGraphEnabled(graphEnabled);
                mapper.insertDocument(document);
            }
        }
        if ("pending".equals(document.getParseStatus())) {
            throw new ServiceException("该文档已有版本正在解析", HttpStatus.CONFLICT);
        }

        KbDocument.Version version = new KbDocument.Version();
        version.setDocId(document.getDocId());
        version.setLibraryId(scope.getLibraryId());
        version.setDeptId(scope.getDeptId());
        version.setVersion(label);
        version.setFilename(filename);
        version.setSize(file.getSize());
        try {
            mapper.insertVersion(version);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("该文档已有此版本号，请填写新版本号", HttpStatus.CONFLICT);
        }

        try {
            Path original = storage.store(version, file);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            Files.deleteIfExists(original);
                        } catch (IOException e) {
                            log.warn("清理上传失败的原件时出错，版本 {}", version.getVersionId(), e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            log.warn("保存知识库原件失败，库 {}", libraryId, e);
            throw new ServiceException("原件保存失败，请检查私有存储配置");
        }
        mapper.saveOriginal(version);
        mapper.enqueueParse(version.getVersionId());
    }

    public Map<String, Object> preview(long libraryId, long versionId, int page) {
        KbDocument.Library scope = scope(libraryId, false);
        KbDocument.Version version = version(scope, versionId);
        if (!"completed".equals(version.getParseStatus())) {
            throw new ServiceException("正文尚未解析成功", HttpStatus.CONFLICT);
        }
        if (page < 1 || page > 100_000) {
            throw new ServiceException("预览页码无效", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chunks", mapper.listChunks(scope, versionId, (page - 1) * PREVIEW_PAGE_SIZE, PREVIEW_PAGE_SIZE));
        result.put("total", mapper.countChunks(scope, versionId));
        result.put("pageSize", PREVIEW_PAGE_SIZE);
        return result;
    }

    public KbDocument.Version downloadVersion(long libraryId, long versionId) {
        return version(scope(libraryId, false), versionId);
    }

    public Path openOriginal(KbDocument.Version version) {
        try {
            Path path = storage.original(version);
            if (Files.isRegularFile(path)) {
                return path;
            }
        } catch (IOException e) {
            log.warn("读取知识库原件失败，版本 {}", version.getVersionId(), e);
        }
        throw new ServiceException("原件暂不可下载", HttpStatus.NOT_FOUND);
    }

    @Transactional
    public void retry(long libraryId, long versionId) {
        KbDocument.Library scope = scope(libraryId, true);
        KbDocument.Version version = version(scope, versionId);
        KbDocument document = document(scope, version.getDocId());
        if (!document.getLatestVersionId().equals(versionId) || mapper.retryParse(versionId) != 1) {
            throw new ServiceException("只能重试最后一次失败的解析", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public void delete(long libraryId, long docId) {
        KbDocument.Library scope = scope(libraryId, true);
        document(scope, docId);
        mapper.deleteChunks(scope, docId);
        mapper.deleteTasks(docId);
        mapper.deleteVersions(docId);
        mapper.deleteDocument(scope, docId);
    }

    private KbDocument.Library scope(long libraryId, boolean maintain) {
        KbDocument.Library scope = mapper.findLibrary(libraryId, SecurityUtils.getUserId(), permissions.hasRole("admin"), maintain);
        if (scope == null || (maintain && !scope.isMaintain())) {
            throw new ServiceException("没有权限访问或维护此部门知识库", HttpStatus.FORBIDDEN);
        }
        return scope;
    }

    private KbDocument document(KbDocument.Library scope, long docId) {
        KbDocument document = mapper.findDocument(scope, docId);
        if (document == null) {
            throw new ServiceException("文档不存在或已删除", HttpStatus.NOT_FOUND);
        }
        return document;
    }

    private KbDocument.Version version(KbDocument.Library scope, long versionId) {
        KbDocument.Version version = mapper.findVersion(scope, versionId);
        if (version == null) {
            throw new ServiceException("文档版本不存在或已删除", HttpStatus.NOT_FOUND);
        }
        return version;
    }

    static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String filename(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException("请选择非空文件，单文件最大 50 MB", HttpStatus.BAD_REQUEST);
        }
        String name = file.getOriginalFilename();
        name = name == null ? "" : name.replace('\\', '/');
        name = text(name.substring(name.lastIndexOf('/') + 1), 255, "文件名称");
        if (!EXTENSIONS.contains(extension(name))) {
            throw new ServiceException("请上传 TXT、MD、CSV、PDF、DOCX、XLSX 或 HTML 文件", HttpStatus.BAD_REQUEST);
        }
        return name;
    }

    private static String text(String input, int limit, String field) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty() || value.codePointCount(0, value.length()) > limit
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new ServiceException(field + "格式无效，最多 " + limit + " 个字符", HttpStatus.BAD_REQUEST);
        }
        return value;
    }
}
