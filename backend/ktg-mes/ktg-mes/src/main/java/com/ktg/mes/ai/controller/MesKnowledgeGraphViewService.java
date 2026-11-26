package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MesKnowledgeGraphViewService {

    private static final int BROWSE_LIMIT = 100;
    private static final int MAX_FACT_IDS = 24;

    private static final Pattern FACT_ID = Pattern.compile(
            "[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"
    );

    private static final String[] GRAPH_FIELDS = {
            "sourceId", "sourceName", "sourceKind",
            "targetId", "targetName", "targetKind",
            "factId", "predicate", "documentId", "versionId", "location", "evidence"
    };

    private static final String GRAPH_QUERY = String.join("\n",
            "MATCH (s:KbEntity)-[:SUBJECT_OF]->(f:KbFact)-[:OBJECT]->(o:KbEntity)",
            "WHERE s.ownerId = $ownerId",
            "  AND f.ownerId = $ownerId",
            "  AND o.ownerId = $ownerId",
            "  AND f.versionId IN $versionIds",
            "  AND ($factIds = [] OR f.factKey IN $factIds)",
            "RETURN elementId(s) AS sourceId, s.name AS sourceName, s.kind AS sourceKind,",
            "       elementId(o) AS targetId, o.name AS targetName, o.kind AS targetKind,",
            "       f.factKey AS factId, f.predicate AS predicate,",
            "       f.documentId AS documentId, f.versionId AS versionId,",
            "       f.location AS location, f.evidence AS evidence",
            "ORDER BY f.versionId DESC, f.chunkNo, f.factKey",
            "LIMIT $limit"
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final MesKnowledgeGraphStore graphStore;

    public MesKnowledgeGraphViewService(JdbcTemplate jdbc, ObjectMapper json,
                                        MesKnowledgeGraphStore graphStore) {
        this.jdbc = jdbc;
        this.json = json;
        this.graphStore = graphStore;
    }

    public ObjectNode view(long ownerId, String rawFactIds) {
        List<String> factIds = parseFactIds(rawFactIds);
        Map<Long, Map<String, Object>> versions = MesKnowledgeActiveVersions.load(jdbc, ownerId);

        ObjectNode result = json.createObjectNode();
        ArrayNode nodes = result.putArray("nodes");
        ArrayNode links = result.putArray("links");

        if (versions.isEmpty()) {
            return result;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("ownerId", ownerId);
        params.put("versionIds", new ArrayList<>(versions.keySet()));
        params.put("factIds", factIds);
        params.put("limit", factIds.isEmpty() ? BROWSE_LIMIT : factIds.size());

        JsonNode data = graphStore.query(GRAPH_QUERY, params);
        JsonNode fields = data.path("fields");
        if (!fields.isArray()) {
            throw new IllegalStateException("Neo4j 图谱查询未返回列名");
        }

        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            columns.put(fields.get(i).asText(), i);
        }
        for (String field : GRAPH_FIELDS) {
            if (!columns.containsKey(field)) {
                throw new IllegalStateException("Neo4j 图谱查询缺少列：" + field);
            }
        }

        Set<String> nodeIds = new HashSet<>();
        Set<String> linkIds = new HashSet<>();

        for (JsonNode row : data.path("values")) {
            if (!row.isArray() || row.size() != fields.size()) {
                throw new IllegalStateException("Neo4j 图谱查询返回了格式错误的数据行");
            }

            long versionId = row.path(columns.get("versionId")).asLong(-1);
            long documentId = row.path(columns.get("documentId")).asLong(-1);
            Map<String, Object> version = versions.get(versionId);

            if (version == null
                    || documentId != ((Number) version.get("document_id")).longValue()) {
                continue;
            }

            String source = row.path(columns.get("sourceId")).asText("");
            String target = row.path(columns.get("targetId")).asText("");
            String factId = row.path(columns.get("factId")).asText("");

            if (source.isEmpty() || target.isEmpty() || factId.isEmpty() || !linkIds.add(factId)) {
                continue;
            }

            addNode(nodes, nodeIds, source,
                    row.path(columns.get("sourceName")).asText(""),
                    row.path(columns.get("sourceKind")).asText(""));
            addNode(nodes, nodeIds, target,
                    row.path(columns.get("targetName")).asText(""),
                    row.path(columns.get("targetKind")).asText(""));

            links.addObject()
                    .put("id", factId)
                    .put("source", source)
                    .put("target", target)
                    .put("label", row.path(columns.get("predicate")).asText(""))
                    .put("fileName", (String) version.get("original_name"))
                    .put("version", ((Number) version.get("version_no")).intValue())
                    .put("location", row.path(columns.get("location")).asText(""))
                    .put("evidence", row.path(columns.get("evidence")).asText(""));
        }
        return result;
    }

    private void addNode(ArrayNode nodes, Set<String> nodeIds,
                         String id, String name, String kind) {
        if (nodeIds.add(id)) {
            nodes.addObject().put("id", id).put("name", name).put("kind", kind);
        }
    }

    private List<String> parseFactIds(String rawFactIds) {
        if (rawFactIds == null) {
            return Collections.emptyList();
        }

        String[] parts = rawFactIds.split(",", MAX_FACT_IDS + 1);
        if (parts.length > MAX_FACT_IDS) {
            throw new IllegalArgumentException("factIds 最多允许 24 个事实 ID");
        }

        List<String> factIds = new ArrayList<>(parts.length);
        for (String part : parts) {
            String id = part.trim();
            if (!FACT_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("factIds 必须是逗号分隔的 UUID");
            }
            factIds.add(id.toLowerCase(Locale.ROOT));
        }
        return factIds;
    }
}