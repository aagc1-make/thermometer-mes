package com.ktg.mes.ai.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.KbDocumentMapper;
import com.ktg.mes.ai.mapper.KbGraphMapper;
import com.ktg.mes.ai.mapper.KbRetrievalMapper;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ktg.mes.ai.service.KbRagEngines.object;

@Service
public class KbGraphService {
    private final KbGraphMapper mapper;
    private final KbDocumentMapper documents;
    private final KbRetrievalMapper retrieval;
    private final KbNeo4j neo;

    public KbGraphService(KbGraphMapper mapper, KbDocumentMapper documents,
                          KbRetrievalMapper retrieval, KbNeo4j neo) {
        this.mapper = mapper;
        this.documents = documents;
        this.retrieval = retrieval;
        this.neo = neo;
    }

    public Graph browse(KbDocument.Library scope, String search, String focus, int hops,
                        List<String> types, String relation) throws IOException {
        if (search.length() > 100 || focus.length() > 120 || types.size() > 10 || relation.length() > 24) {
            throw new ServiceException("图谱筛选参数超出范围。", HttpStatus.BAD_REQUEST);
        }
        List<Long> versions = mapper.visibleVersions(scope);
        if (versions.isEmpty()) return new Graph();
        JsonNode rows = focus.isEmpty() ? neo.find(scope, versions, search, types, relation, Collections.emptyList())
            : neo.expand(scope, versions, focus, hops, types, relation);
        return collect(scope, Collections.singletonList(rows), 200, 400);
    }

    Graph sample(KbDocument.Library scope, List<Long> evidence) throws IOException {
        List<Long> versions = mapper.visibleVersions(scope);
        if (versions.isEmpty() || evidence.isEmpty()) return new Graph();
        JsonNode rows = neo.find(scope, versions, "", Collections.emptyList(), "", evidence);
        return collect(scope, Collections.singletonList(rows), 200, 400);
    }

    Graph forQuestion(KbDocument.Library scope, String question) throws IOException {
        if (!neo.configured()) throw new IOException("Neo4j is not configured");
        List<Long> versions = mapper.visibleVersions(scope);
        if (versions.isEmpty()) return new Graph();
        List<JsonNode> neighbors = new ArrayList<>();
        for (JsonNode row : neo.linkQuestion(scope, versions, question)) {
            neighbors.add(neo.expand(scope, versions, row.get(0).asText(), 2, Collections.emptyList(), ""));
        }
        return collect(scope, neighbors, 28, 48);
    }

    public void retry(KbDocument.Library scope, long versionId) {
        if (!scope.isMaintain()) throw new ServiceException("当前账号不能维护此知识库。", HttpStatus.FORBIDDEN);
        KbDocument.Version version = documents.findVersion(scope, versionId);
        if (version == null || !version.isActive() || !version.isGraphEnabled() || mapper.retry(versionId) == 0) {
            throw new ServiceException("该版本没有可重试的图谱任务。", HttpStatus.BAD_REQUEST);
        }
    }

    private Graph collect(KbDocument.Library scope, List<JsonNode> tables, int nodeLimit, int edgeLimit) {
        Graph graph = new Graph();
        Map<String, Node> nodes = new LinkedHashMap<>();
        Map<String, Edge> edges = new LinkedHashMap<>();
        for (JsonNode table : tables) {
            for (JsonNode row : table) {
                String from = row.get(0).asText();
                String to = row.get(4).asText();
                String relation = row.get(8).asText();
                String key = from + "|" + relation + "|" + to;
                int newNodes = (nodes.containsKey(from) ? 0 : 1) + (nodes.containsKey(to) ? 0 : 1);
                if (nodes.size() + newNodes > nodeLimit || (!edges.containsKey(key) && edges.size() == edgeLimit)) {
                    graph.limited = true;
                    continue;
                }
                Node left = nodes.computeIfAbsent(from, id -> new Node(row, 0));
                Node right = nodes.computeIfAbsent(to, id -> new Node(row, 4));
                Edge edge = edges.computeIfAbsent(key, id -> new Edge(left, right, relation));
                for (JsonNode evidence : row.get(9)) {
                    if (edge.evidence.size() < 8) edge.evidence.add(evidence.asLong());
                    if (left.evidence.size() < 8) left.evidence.add(evidence.asLong());
                    if (right.evidence.size() < 8) right.evidence.add(evidence.asLong());
                }
            }
        }
        List<Long> ids = edges.values().stream().flatMap(edge -> edge.evidence.stream()).distinct()
            .collect(Collectors.toList());
        if (ids.isEmpty()) return graph;
        Map<Long, KbCitation> visible = retrieval.activeHits(scope, ids).stream()
            .collect(Collectors.toMap(KbCitation::getChunkId, hit -> hit));
        Set<String> linked = new LinkedHashSet<>();
        for (Edge edge : edges.values()) {
            if (edge.evidence.stream().noneMatch(visible::containsKey)) continue;
            linked.add(edge.source);
            linked.add(edge.target);
            graph.edges.add(edge);
        }
        for (Node node : nodes.values()) {
            if (!linked.contains(node.id)) continue;
            node.sources = node.evidence.stream().map(visible::get).filter(java.util.Objects::nonNull)
                .map(KbGraphService::source).collect(Collectors.toList());
            graph.nodes.add(node);
        }
        graph.citations = ids.stream().map(visible::get).filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
        return graph;
    }

    private static Map<String, Object> source(KbCitation hit) {
        String text = hit.getContent();
        return object("libraryId", hit.getLibraryId(), "versionId", hit.getVersionId(), "title", hit.getTitle(),
            "sectionLabel", hit.getSectionLabel(), "chunkIndex", hit.getChunkIndex(),
            "excerpt", text.length() <= 140 ? text : text.substring(0, 140) + "…");
    }

    @Getter
    public static class Graph {
        private final List<Node> nodes = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private boolean limited;
        @JsonIgnore
        private List<KbCitation> citations = Collections.emptyList();
    }

    @Getter
    public static class Node {
        private final String id;
        private final String name;
        private final String type;
        private final String code;
        private final Set<Long> evidence = new LinkedHashSet<>();
        private List<Map<String, Object>> sources;

        Node(JsonNode row, int start) {
            id = row.get(start).asText();
            name = row.get(start + 1).asText();
            type = row.get(start + 2).asText();
            code = row.get(start + 3).asText(null);
        }
    }

    @Getter
    public static class Edge {
        private final String source;
        private final String target;
        private final String kind;
        private final String fromName;
        private final String toName;
        private final Set<Long> evidence = new LinkedHashSet<>();

        Edge(Node from, Node to, String kind) {
            source = from.id;
            target = to.id;
            fromName = from.name;
            toName = to.name;
            this.kind = kind;
        }
    }
}
