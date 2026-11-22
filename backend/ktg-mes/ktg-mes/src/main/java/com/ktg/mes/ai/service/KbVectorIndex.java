package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.KbDocumentMapper;
import com.ktg.mes.ai.mapper.KbRetrievalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ktg.mes.ai.service.KbRagEngines.object;

@Component
public class KbVectorIndex {
    private static final Logger log = LoggerFactory.getLogger(KbVectorIndex.class);
    private final KbRagEngines engines;
    private final KbRetrievalMapper retrieval;
    private final KbDocumentMapper documents;

    public KbVectorIndex(KbRagEngines engines, KbRetrievalMapper retrieval, KbDocumentMapper documents) {
        this.engines = engines;
        this.retrieval = retrieval;
        this.documents = documents;
    }

    List<Long> search(KbDocument.Library scope, String question) throws IOException {
        if (!engines.indexingAvailable()) {
            throw new IOException("Qdrant or the embedding model is unavailable");
        }
        String path = engines.collectionPath();
        engines.qdrant("GET", path, null, 1500);
        JsonNode query = engines.embed(Collections.singletonList(question)).get(0);
        List<Object> match = Arrays.asList(match("dept_id", scope.getDeptId()), match("library_id", scope.getLibraryId()));
        JsonNode hits = engines.qdrant("POST", path + "/points/query", object(
            "query", query, "filter", object("must", match), "limit", 12, "score_threshold", 0.45
        ), 3500).path("result").path("points");
        List<Long> ids = new ArrayList<>();
        for (JsonNode point : hits) {
            if (point.path("id").canConvertToLong()) {
                ids.add(point.path("id").asLong());
            }
        }
        return ids;
    }

    @Scheduled(initialDelay = 6000, fixedDelay = 6000)
    public void updateIndex() {
        if (!engines.indexingAvailable()) {
            return;
        }
        retirePreviousVersion();
        KbDocument.Version version = retrieval.nextIndexVersion();
        if (version == null) {
            return;
        }
        if (version.getTaskId() == null) {
            retrieval.startIndex(version);
        } else {
            retrieval.restartIndex(version.getTaskId());
        }
        try {
            writeVersion(version);
            documents.finishParse(version.getTaskId(), "completed", null);
        } catch (IOException | RuntimeException e) {
            log.warn("知识库向量写入未完成，文档版本 {}", version.getVersionId(), e);
            documents.finishParse(version.getTaskId(), "failed", "向量服务暂不可用，系统会自动重试。");
        }
    }

    private void writeVersion(KbDocument.Version version) throws IOException {
        long after = 0;
        List<KbCitation> batch;
        String path = engines.collectionPath();
        while (!(batch = retrieval.indexChunks(version, after)).isEmpty()) {
            List<String> texts = batch.stream().map(chunk -> chunk.getTitle() + "\n" + chunk.getContent()).collect(Collectors.toList());
            JsonNode vectors = engines.embed(texts);
            if (after == 0) {
                ensureCollection(path, vectors.get(0).size());
            }
            List<Map<String, Object>> points = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                KbCitation chunk = batch.get(i);
                points.add(object("id", chunk.getChunkId(), "vector", vectors.get(i), "payload", object(
                    "dept_id", chunk.getDeptId(), "library_id", chunk.getLibraryId(), "version_id", chunk.getVersionId()
                )));
                after = chunk.getChunkId();
            }
            engines.qdrant("PUT", path + "/points?wait=true", object("points", points), 10000);
        }
    }

    private void ensureCollection(String path, int dimensions) throws IOException {
        try {
            engines.qdrant("GET", path, null, 1800);
        } catch (KbRagEngines.HttpFailure e) {
            if (e.status != 404) {
                throw e;
            }
            engines.qdrant("PUT", path, object("vectors", object("size", dimensions, "distance", "Cosine")), 10000);
        }
    }

    private void retirePreviousVersion() {
        KbDocument.Version version = retrieval.nextRetiredVersion();
        if (version == null) {
            return;
        }
        try {
            String path = engines.collectionPath() + "/points/delete?wait=true";
            engines.qdrant("POST", path, object("filter", object("must", Arrays.asList(
                match("dept_id", version.getDeptId()), match("library_id", version.getLibraryId()),
                match("version_id", version.getVersionId())
            ))), 10000);
        } catch (KbRagEngines.HttpFailure e) {
            if (e.status != 404) {
                return;
            }
        } catch (IOException e) {
            return;
        }
        retrieval.removeIndexRecord(version.getTaskId());
    }

    private static Map<String, Object> match(String key, Object value) {
        return object("key", key, "match", object("value", value));
    }
}
