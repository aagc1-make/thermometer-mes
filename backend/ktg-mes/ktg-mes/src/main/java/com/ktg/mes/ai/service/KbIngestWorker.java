package com.ktg.mes.ai.service;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.KbDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;

@Component
@EnableScheduling
public class KbIngestWorker {
    private static final Logger log = LoggerFactory.getLogger(KbIngestWorker.class);
    private static final int INSERT_BATCH = 100;
    private final KbDocumentMapper mapper;
    private final KbDocumentParser parser;
    private final KbOriginalStorage storage;
    private final TransactionTemplate transaction;

    public KbIngestWorker(KbDocumentMapper mapper, KbDocumentParser parser, KbOriginalStorage storage,
                         PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.parser = parser;
        this.storage = storage;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelay = 1500, initialDelay = 1500)
    public void parseNext() {
        Long versionId = mapper.nextPendingVersion();
        if (versionId == null) {
            return;
        }
        KbDocument.Version version = mapper.findPending(versionId);
        if (version == null) {
            return;
        }
        try {
            KbDocumentParser.Parsed parsed = parser.parse(storage.original(version),
                KbDocumentService.extension(version.getFilename()));
            transaction.executeWithoutResult(status -> publish(version, parsed));
        } catch (ServiceException e) {
            fail(version, e.getMessage());
        } catch (IOException | RuntimeException e) {
            log.warn("知识库文档解析失败，版本 {}", versionId, e);
            fail(version, "文件无法解析，请确认格式正确、内容完整，并且未加密。");
        }
    }

    private void publish(KbDocument.Version version, KbDocumentParser.Parsed parsed) {
        KbDocument.Library scope = new KbDocument.Library();
        scope.setDeptId(version.getDeptId());
        scope.setLibraryId(version.getLibraryId());
        if (!stillPending(version)) {
            return;
        }
        long newTotal = mapper.countCharacters(scope) - mapper.activeCharacters(version.getDocId()) + parsed.characters;
        if (newTotal > KbDocumentParser.CHARACTER_LIMIT) {
            throw new ServiceException("解析结果超过部门库 2000 万字符配额。");
        }
        for (int from = 0; from < parsed.chunks.size(); from += INSERT_BATCH) {
            mapper.insertChunks(version, parsed.chunks.subList(from, Math.min(from + INSERT_BATCH, parsed.chunks.size())));
        }
        version.setChars(parsed.characters);
        mapper.deactivateVersions(version.getDocId());
        mapper.publishVersion(version);
        mapper.publishDocument(version);
        mapper.finishParse(version.getTaskId(), "completed", null);
    }

    private boolean stillPending(KbDocument.Version version) {
        return mapper.lockLibrary(version.getLibraryId(), version.getDeptId()) != null
            && mapper.findPending(version.getVersionId()) != null;
    }

    private void fail(KbDocument.Version version, String reason) {
        transaction.executeWithoutResult(status -> {
            if (stillPending(version)) {
                mapper.finishParse(version.getTaskId(), "failed", reason);
            }
        });
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void purgeDeletedOriginals() {
        for (KbDocument.Version version : mapper.expiredOriginals()) {
            try {
                Files.deleteIfExists(storage.original(version));
                mapper.clearOriginal(version.getVersionId());
            } catch (IOException e) {
                log.warn("清理已删除文档原件失败，版本 {}", version.getVersionId(), e);
            }
        }
    }
}
