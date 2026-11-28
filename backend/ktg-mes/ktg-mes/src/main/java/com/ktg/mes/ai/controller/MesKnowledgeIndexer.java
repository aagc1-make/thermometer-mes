package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import java.util.List;

@Service
public class MesKnowledgeIndexer {

    private static final String COLLECTION = "http://127.0.0.1:6333/collections/mes_knowledge";
    private static final int VECTOR_SIZE = 1024;

    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http;

    public MesKnowledgeIndexer() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(120000);
        this.http = new RestTemplate(factory);
    }

    public void index(long ownerId, long documentId, long versionId,
                      List<MesKnowledgeChunks.Chunk> fragments) {
        ensureCollection();
        for (int start = 0; start < fragments.size(); start += 16) {
            int end = Math.min(start + 16, fragments.size());
            List<MesKnowledgeChunks.Chunk> batch = fragments.subList(start, end);

            ObjectNode request = json.createObjectNode();
            request.put("model", "bge-m3");
            ArrayNode input = request.putArray("input");
            for (MesKnowledgeChunks.Chunk fragment : batch) {
                input.add(fragment.getText());
            }

            JsonNode embedding = http.postForObject(
                    "http://127.0.0.1:11434/api/embed", request, JsonNode.class);
            if (embedding == null || !embedding.path("embeddings").isArray()
                    || embedding.path("embeddings").size() != batch.size()) {
                throw new IllegalStateException("Ollama 返回的向量数量不正确");
            }

            ObjectNode upsert = json.createObjectNode();
            ArrayNode points = upsert.putArray("points");
            for (int i = 0; i < batch.size(); i++) {
                MesKnowledgeChunks.Chunk fragment = batch.get(i);
                JsonNode vector = embedding.path("embeddings").get(i);
                if (!vector.isArray() || vector.size() != VECTOR_SIZE) {
                    throw new IllegalStateException("向量维度不是 1024");
                }

                int chunkNo = fragment.getChunkNo();
                String key = versionId + ":" + chunkNo;
                ObjectNode point = points.addObject();
                point.put("id", UUID.nameUUIDFromBytes(
                        key.getBytes(StandardCharsets.UTF_8)).toString());
                point.set("vector", vector);

                ObjectNode payload = point.putObject("payload");
                payload.put("owner_id", ownerId);
                payload.put("document_id", documentId);
                payload.put("version_id", versionId);
                payload.put("chunk_no", chunkNo);
                payload.put("text", fragment.getText());
                payload.put("location", fragment.getLocation());
            }

            JsonNode result = http.exchange(
                    COLLECTION + "/points?wait=true",
                    HttpMethod.PUT,
                    new HttpEntity<>(upsert),
                    JsonNode.class).getBody();

            if (result == null || !"ok".equals(result.path("status").asText())
                    || !"completed".equals(result.path("result").path("status").asText())) {
                throw new IllegalStateException("Qdrant 未确认向量写入完成");
            }
        }
    }

    private void ensureCollection() {
        try {
            http.getForObject(COLLECTION, JsonNode.class);
            return;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }

        ObjectNode request = json.createObjectNode();
        request.putObject("vectors").put("size", VECTOR_SIZE).put("distance", "Cosine");
        try {
            JsonNode result = http.exchange(COLLECTION, HttpMethod.PUT,
                    new HttpEntity<>(request), JsonNode.class).getBody();
            if (result == null || !"ok".equals(result.path("status").asText())) {
                throw new IllegalStateException("Qdrant 未确认集合创建完成");
            }
        } catch (HttpClientErrorException e) {
            // 并发上传时另一个请求可能已经建好集合。
            if (e.getStatusCode() != HttpStatus.CONFLICT) {
                throw e;
            }
        }
    }

    public void deleteDocument(long ownerId, long documentId) {
        ObjectNode request = json.createObjectNode();
        ArrayNode must = request.putObject("filter").putArray("must");

        must.addObject().put("key", "owner_id")
                .putObject("match").put("value", ownerId);
        must.addObject().put("key", "document_id")
                .putObject("match").put("value", documentId);

        JsonNode result = http.postForObject(
                COLLECTION + "/points/delete?wait=true",
                request,
                JsonNode.class);

        if (result == null || !"ok".equals(result.path("status").asText())
                || !"completed".equals(result.path("result").path("status").asText())) {
            throw new IllegalStateException("Qdrant 未确认删除完成");
        }
    }
}