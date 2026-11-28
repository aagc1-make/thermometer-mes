package com.ktg.mes.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class MesKnowledgeGraphIndexer {

    private static final Logger log = LoggerFactory.getLogger(MesKnowledgeGraphIndexer.class);

    private static final String FACT_EXTRACTION_PROMPT = String.join("\n",
            "只从本次提供的原文中抽取明确陈述的事实。",
            "返回 JSON 对象，包含 facts 数组。每条事实包含 subjectType、subject、predicate、objectType、object、evidence。",
            "实体类型只使用产品、工序、部件、检验项。关系只使用包含、安装、需要检验。",
            "某部件的来料检验项目包括甲和乙，表示该部件分别需要检验甲、乙，输出两条事实。",
            "描述某工序安装某部件时，工序是主体，部件是客体。",
            "evidence 逐字复制本次原文中同时包含主体和客体的句子。没有符合条件的事实时返回 {\"facts\":[]}。",
            "示例输入：温度探头的来料检验项目包括测量精度和外观。",
            "示例输出：{\"facts\":["
                    + "{\"subjectType\":\"部件\",\"subject\":\"温度探头\",\"predicate\":\"需要检验\","
                    + "\"objectType\":\"检验项\",\"object\":\"测量精度\","
                    + "\"evidence\":\"温度探头的来料检验项目包括测量精度和外观。\"},"
                    + "{\"subjectType\":\"部件\",\"subject\":\"温度探头\",\"predicate\":\"需要检验\","
                    + "\"objectType\":\"检验项\",\"object\":\"外观\","
                    + "\"evidence\":\"温度探头的来料检验项目包括测量精度和外观。\"}]}",
            "示例输入：电子温度计在组装工序安装温度探头。",
            "示例输出：{\"facts\":[{\"subjectType\":\"工序\",\"subject\":\"组装工序\","
                    + "\"predicate\":\"安装\",\"objectType\":\"部件\",\"object\":\"温度探头\","
                    + "\"evidence\":\"电子温度计在组装工序安装温度探头。\"}]}");

    private static final String REPLACE_VERSION_GRAPH = String.join("\n",
            "CALL () {",
            "    UNWIND $fragments AS fragment",
            "    WITH fragment ORDER BY fragment.chunkNo",
            "    MERGE (c:KbChunk {ownerId: $ownerId, versionId: $versionId, chunkNo: fragment.chunkNo})",
            "    SET c.documentId = $documentId, c.text = fragment.text, c.location = fragment.location",
            "    RETURN count(c) AS chunksWritten",
            "}",
            "CALL () {",
            "    UNWIND $rows AS row",
            "    MERGE (s:KbEntity {ownerId: $ownerId, kind: row.subjectType, name: row.subject})",
            "    MERGE (o:KbEntity {ownerId: $ownerId, kind: row.objectType, name: row.object})",
            "    MERGE (f:KbFact {ownerId: $ownerId, factKey: row.factKey})",
            "    SET f.predicate = row.predicate, f.documentId = $documentId,",
            "        f.versionId = $versionId, f.chunkNo = row.chunkNo,",
            "        f.location = row.location, f.evidence = row.evidence",
            "    MERGE (s)-[:SUBJECT_OF]->(f)",
            "    MERGE (f)-[:OBJECT]->(o)",
            "    RETURN count(DISTINCT f) AS factsWritten",
            "}",
            "WITH chunksWritten, factsWritten, [row IN $rows | row.factKey] AS factKeys",
            "OPTIONAL MATCH (old:KbFact {ownerId: $ownerId, documentId: $documentId, versionId: $versionId})",
            "WHERE old.factKey IS NULL OR NOT (old.factKey IN factKeys)",
            "WITH chunksWritten, factsWritten, collect(old) AS staleFacts",
            "FOREACH (old IN staleFacts | DETACH DELETE old)",
            "RETURN chunksWritten, factsWritten, size(staleFacts) AS factsRemoved"
    );

    private final MesKnowledgeGraphStore graphStore;

    private final ObjectMapper json;
    private final RestTemplate ollama;

    private final String chatModel;

    public MesKnowledgeGraphIndexer(MesKnowledgeGraphStore graphStore, ObjectMapper json,
                                   @Value("${mes.knowledge.model}") String chatModel) {
        this.graphStore = graphStore;
        this.json = json;
        this.chatModel = chatModel;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(120000);
        this.ollama = new RestTemplate(factory);
    }

    public void index(long ownerId, long documentId, long versionId,
                      List<MesKnowledgeChunks.Chunk> fragments) {
        writeGraph(prepareGraph(ownerId, documentId, versionId, fragments));
    }

    Map<String, Object> prepareGraph(long ownerId, long documentId, long versionId,
                                     List<MesKnowledgeChunks.Chunk> fragments) {
        List<Map<String, Object>> rows = new ArrayList<>(fragments.size());

        for (MesKnowledgeChunks.Chunk fragment : fragments) {
            Map<String, Object> row = new HashMap<>();
            row.put("chunkNo", fragment.getChunkNo());
            row.put("text", fragment.getText());
            row.put("location", fragment.getLocation());
            rows.add(row);
        }

        List<Map<String, Object>> factRows = new ArrayList<>();
        int[] skipped = new int[2];
        for (MesKnowledgeChunks.Chunk fragment : fragments) {
            appendFactRows(factRows, versionId, fragment, skipped);
        }
        if (skipped[0] > 0 || skipped[1] > 0) {
            log.warn("图谱事实抽取跳过不合规内容，文档 ID {}，版本 ID {}，无法解析的片段 {} 个，"
                            + "未通过校验的事实 {} 条",
                    documentId, versionId, skipped[0], skipped[1]);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ownerId", ownerId);
        parameters.put("documentId", documentId);
        parameters.put("versionId", versionId);
        parameters.put("fragments", rows);
        parameters.put("rows", factRows);
        return parameters;
    }

    void writeGraph(Map<String, Object> parameters) {
        graphStore.query(REPLACE_VERSION_GRAPH, parameters);
    }

    private JsonNode extractFacts(String text) {
        ObjectNode request = json.createObjectNode();
        request.put("model", chatModel);
        request.put("format", "json");
        request.put("stream", false);
        request.putObject("options").put("temperature", 0);

        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "system").put("content", FACT_EXTRACTION_PROMPT);
        messages.addObject().put("role", "user").put("content", text);

        JsonNode response = ollama.postForObject(
                "http://127.0.0.1:11434/api/chat", request, JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Ollama 未返回实体抽取结果");
        }

        try {
            JsonNode result = json.readTree(response.path("message").path("content").asText());
            JsonNode facts = result == null ? null : result.path("facts");
            if (facts == null || !facts.isArray()) {
                throw new IllegalStateException("实体抽取结果缺少 facts 数组");
            }
            return facts;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("实体抽取结果不是有效 JSON", e);
        }
    }

    private void appendFactRows(List<Map<String, Object>> rows, long versionId,
                                MesKnowledgeChunks.Chunk fragment, int[] skipped) {
        JsonNode facts;
        try {
            facts = extractFacts(fragment.getText());
        } catch (IllegalStateException e) {
            skipped[0]++;
            log.debug("片段 {} 的图谱事实抽取结果不可用：{}", fragment.getChunkNo(), e.getMessage());
            return;
        }

        for (JsonNode fact : facts) {
            String subjectType = fact.path("subjectType").asText("").trim();
            String subject = fact.path("subject").asText("").trim();
            String predicate = fact.path("predicate").asText("").trim();
            String objectType = fact.path("objectType").asText("").trim();
            String object = fact.path("object").asText("").trim();
            String evidence = fact.path("evidence").asText("").trim();

            if (!subjectType.matches("产品|工序|部件|检验项")
                    || !objectType.matches("产品|工序|部件|检验项")
                    || !predicate.matches("包含|安装|需要检验")
                    || subject.isEmpty() || object.isEmpty() || evidence.isEmpty()
                    || !fragment.getText().contains(evidence)
                    || !evidence.contains(subject) || !evidence.contains(object)) {
                skipped[1]++;
                log.debug("片段 {} 的图谱事实未通过校验，已跳过：{}", fragment.getChunkNo(), fact);
                continue;
            }

            String identity = json.createArrayNode()
                    .add(versionId).add(fragment.getChunkNo())
                    .add(subjectType).add(subject).add(predicate)
                    .add(objectType).add(object).toString();

            Map<String, Object> row = new HashMap<>();
            row.put("factKey", UUID.nameUUIDFromBytes(
                    identity.getBytes(StandardCharsets.UTF_8)).toString());
            row.put("subjectType", subjectType);
            row.put("subject", subject);
            row.put("predicate", predicate);
            row.put("objectType", objectType);
            row.put("object", object);
            row.put("chunkNo", fragment.getChunkNo());
            row.put("location", fragment.getLocation());
            row.put("evidence", evidence);
            rows.add(row);
        }
    }

    public void deleteDocument(long ownerId, long documentId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ownerId", ownerId);
        parameters.put("documentId", documentId);

        graphStore.query(
                "MATCH (n) "
                        + "WHERE (n:KbFact OR n:KbChunk) "
                        + "AND n.ownerId = $ownerId AND n.documentId = $documentId "
                        + "DETACH DELETE n "
                        + "RETURN count(*) AS deleted",
                parameters);
        graphStore.query("MATCH (n:KbEntity {ownerId: $ownerId}) WHERE NOT (n)--() "
                + "DELETE n RETURN count(*) AS deleted", parameters);
    }
}
