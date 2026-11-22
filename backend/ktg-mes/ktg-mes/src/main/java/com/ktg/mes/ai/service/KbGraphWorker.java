package com.ktg.mes.ai.service;

import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.KbDocumentMapper;
import com.ktg.mes.ai.mapper.KbGraphMapper;
import com.ktg.mes.ai.mapper.KbRetrievalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;

@Component
public class KbGraphWorker {
    private static final Logger log = LoggerFactory.getLogger(KbGraphWorker.class);
    private final KbGraphMapper tasks;
    private final KbDocumentMapper documents;
    private final KbRetrievalMapper retrieval;
    private final KbNeo4j neo;
    private final TransactionTemplate transaction;

    public KbGraphWorker(KbGraphMapper tasks, KbDocumentMapper documents, KbRetrievalMapper retrieval,
                         KbNeo4j neo, PlatformTransactionManager transactions) {
        this.tasks = tasks;
        this.documents = documents;
        this.retrieval = retrieval;
        this.neo = neo;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Scheduled(fixedDelay = 2500, initialDelay = 3000)
    public void sync() {
        if (!neo.configured()) return;
        KbDocument.Version candidate = tasks.nextVersion(null);
        if (candidate != null) {
            KbDocument.Version version = transaction.execute(status -> claim(candidate));
            if (version != null) publish(version);
        }
        KbDocument.Version retired = tasks.retiredVersion();
        if (retired != null && "online".equals(neo.status())) {
            try {
                neo.remove(retired);
                tasks.remove(retired.getTaskId());
            } catch (IOException e) {
                log.warn("知识图谱旧版本清理失败，版本 {}：{}", retired.getVersionId(), e.getMessage());
            }
        }
    }

    private KbDocument.Version claim(KbDocument.Version candidate) {
        if (documents.lockLibrary(candidate.getLibraryId(), candidate.getDeptId()) == null) return null;
        KbDocument.Version version = tasks.nextVersion(candidate.getVersionId());
        if (version == null) return null;
        if (version.getTaskId() == null) tasks.start(version);
        else tasks.restart(version.getTaskId());
        return version;
    }

    private void publish(KbDocument.Version version) {
        try {
            if (!"online".equals(neo.status())) throw new IOException("Neo4j unavailable");
            KbGraphExtraction extraction = new KbGraphExtraction(tasks.anchors());
            long after = 0;
            List<KbCitation> chunks = retrieval.indexChunks(version, after);
            while (!chunks.isEmpty()) {
                neo.write(version, extraction.extract(version, chunks));
                after = chunks.get(chunks.size() - 1).getChunkId();
                chunks = retrieval.indexChunks(version, after);
            }
            documents.finishParse(version.getTaskId(), "completed", null);
        } catch (IOException | RuntimeException e) {
            log.warn("知识图谱同步失败，版本 {}：{}", version.getVersionId(), e.getMessage());
            documents.finishParse(version.getTaskId(), "failed", "图谱抽取或同步失败，请核对 Neo4j 服务后重试。");
        }
    }
}
