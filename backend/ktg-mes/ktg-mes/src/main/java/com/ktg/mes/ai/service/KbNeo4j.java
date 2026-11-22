package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ktg.mes.ai.domain.KbDocument;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.ktg.mes.ai.service.KbRagEngines.object;

@Component
public class KbNeo4j {
    private static final String RELATIONSHIP =
        "MATCH (a:KbEntity {scope:$scope})-[r:KB_LINK]->(b:KbEntity {scope:$scope}) ";
    private static final String RETURN_LINKS =
        " RETURN a.id,a.name,a.type,a.code,b.id,b.name,b.type,b.code,r.kind,collect(DISTINCT r.chunkId)[..8] "
        + "ORDER BY CASE WHEN r.kind = 'MENTIONS' THEN 1 ELSE 0 END, a.id,b.id,r.kind LIMIT 401";

    private final KbRagEngines http;
    private final String endpoint;
    private final String authorization;
    private final String namespace;
    private boolean constrained;
    private volatile long checkedAt;
    private volatile boolean online;

    public KbNeo4j(KbRagEngines http, Environment properties) {
        this.http = http;
        String base = properties.getProperty("mes.ai.graph.url", "").trim();
        String database = properties.getProperty("mes.ai.graph.database", "neo4j");
        String user = properties.getProperty("mes.ai.graph.user", "neo4j");
        String password = properties.getProperty("mes.ai.graph.password", "");
        if (!database.matches("[a-zA-Z0-9_.-]+")) throw new IllegalArgumentException("Invalid Neo4j database name");
        endpoint = base.isEmpty() ? null : base.replaceAll("/+$", "") + "/db/" + database + "/query/v2";
        authorization = password.isEmpty() ? null : "Basic " + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.namespace = properties.getProperty("mes.ai.graph.namespace", "mes");
    }

    boolean configured() { return endpoint != null; }

    public String status() {
        if (!configured()) return "not_configured";
        long now = System.currentTimeMillis();
        if (now - checkedAt > 15000) {
            try {
                query("RETURN 1", object(), 1200);
                online = true;
            } catch (IOException e) {
                online = false;
            }
            checkedAt = now;
        }
        return online ? "online" : "offline";
    }

    void write(KbDocument.Version version, List<Map<String, Object>> rows) throws IOException {
        if (rows.isEmpty()) return;
        ensureConstraint();
        query("UNWIND $rows AS row "
            + "MERGE (a:KbEntity {scope:$scope,id:row.from.id}) "
            + "SET a.name=row.from.name,a.type=row.from.type,a.code=row.from.code "
            + "MERGE (b:KbEntity {scope:$scope,id:row.to.id}) "
            + "SET b.name=row.to.name,b.type=row.to.type,b.code=row.to.code "
            + "MERGE (a)-[:KB_LINK {versionId:$version,kind:row.kind,chunkId:row.chunkId}]->(b)",
            object("scope", scope(version.getDeptId(), version.getLibraryId()),
                "version", version.getVersionId(), "rows", rows), 8000);
    }

    void remove(KbDocument.Version version) throws IOException {
        Map<String, Object> parameters = object("scope", scope(version.getDeptId(), version.getLibraryId()),
            "version", version.getVersionId());
        query(RELATIONSHIP + "WHERE r.versionId=$version DELETE r", parameters, 8000);
        query("MATCH (n:KbEntity {scope:$scope}) WHERE NOT (n)-[:KB_LINK]-() DELETE n", parameters, 8000);
    }

    JsonNode find(KbDocument.Library library, List<Long> versions, String search, List<String> types, String kind,
                  List<Long> evidence)
        throws IOException {
        return query(RELATIONSHIP + "WHERE r.versionId IN $versions "
            + "AND (size($evidence)=0 OR r.chunkId IN $evidence) "
            + "AND ($kind='' OR r.kind=$kind) AND (size($types)=0 OR a.type IN $types OR b.type IN $types) "
            + "AND ($search='' OR any(value IN [a.name,a.code,b.name,b.code] "
            + "WHERE toLower(coalesce(value,'')) CONTAINS toLower($search)))" + RETURN_LINKS,
            object("scope", scope(library), "versions", versions, "search", search, "types", types,
                "kind", kind, "evidence", evidence), 5000);
    }

    JsonNode expand(KbDocument.Library library, List<Long> versions, String focus, int hops,
                    List<String> types, String kind) throws IOException {
        String distance = hops == 1 ? "1" : "1..2";
        return query("MATCH p=(center:KbEntity {scope:$scope,id:$focus})-[:KB_LINK*" + distance + "]-(other:KbEntity) "
            + "WHERE all(edge IN relationships(p) WHERE edge.versionId IN $versions) "
            + "AND all(n IN nodes(p) WHERE n.scope=$scope) "
            + "WITH p LIMIT 600 UNWIND relationships(p) AS r WITH DISTINCT r "
            + "WITH r,startNode(r) AS a,endNode(r) AS b "
            + "WHERE ($kind='' OR r.kind=$kind) AND (size($types)=0 OR a.type IN $types OR b.type IN $types) "
            + RETURN_LINKS,
            object("scope", scope(library), "versions", versions, "focus", focus, "types", types, "kind", kind), 5000);
    }

    JsonNode linkQuestion(KbDocument.Library library, List<Long> versions, String question) throws IOException {
        return query("MATCH (n:KbEntity {scope:$scope}) WHERE n.type<>'Document' "
            + "AND ((size(n.name)>=2 AND toLower($question) CONTAINS toLower(n.name)) "
            + "OR (size(n.code)>=2 AND toLower($question) CONTAINS toLower(n.code))) "
            + "AND EXISTS { MATCH (n)-[r:KB_LINK]-(x:KbEntity {scope:$scope}) WHERE r.versionId IN $versions } "
            + "RETURN n.id ORDER BY size(n.name) DESC,n.id LIMIT 3",
            object("scope", scope(library), "versions", versions, "question", question), 4500);
    }

    private synchronized void ensureConstraint() throws IOException {
        if (!constrained) {
            query("CREATE CONSTRAINT mes_kb_entity IF NOT EXISTS "
                + "FOR (n:KbEntity) REQUIRE (n.scope,n.id) IS UNIQUE", object(), 15000);
            constrained = true;
        }
    }

    private String scope(KbDocument.Library library) { return scope(library.getDeptId(), library.getLibraryId()); }
    private String scope(long deptId, long libraryId) { return namespace + ":" + deptId + ":" + libraryId; }

    private JsonNode query(String statement, Map<String, Object> parameters, int timeout) throws IOException {
        if (!configured()) throw new IOException("Neo4j is not configured");
        JsonNode result = http.request("POST", endpoint, object("statement", statement, "parameters", parameters),
            authorization == null ? null : "Authorization", authorization, timeout);
        if (result == null || result.path("errors").size() > 0) {
            String code = result == null ? "empty response" : result.path("errors").path(0).path("code").asText("query failed");
            throw new IOException("Neo4j: " + code);
        }
        return result.path("data").path("values");
    }
}
