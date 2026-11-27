package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class MesKnowledgeGraphStore {

    private final ObjectMapper json;
    private final RestTemplate http;
    private final String endpoint;
    private final String username;
    private final String password;

    public MesKnowledgeGraphStore(
            ObjectMapper json,
            @Value("${MES_NEO4J_URL:${mes.ai.graph.url:}}") String baseUrl,
            @Value("${MES_NEO4J_DATABASE:${mes.ai.graph.database:neo4j}}") String database,
            @Value("${MES_NEO4J_USER:${mes.ai.graph.user:neo4j}}") String username,
            @Value("${MES_NEO4J_PASSWORD:${mes.ai.graph.password:}}") String password) {
        this.json = json;
        String graphUrl = baseUrl.trim().isEmpty() ? "http://127.0.0.1:7474" : baseUrl.trim();
        this.endpoint = graphUrl.replaceAll("/+$", "")
                + "/db/" + database + "/query/v2";
        this.username = username;
        this.password = password;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.http = new RestTemplate(factory);
    }

    public JsonNode query(String statement, Map<String, ?> parameters) {
        if (password.isEmpty()) {
            throw new IllegalStateException("请设置 MES_NEO4J_PASSWORD 或 MES_AI_NEO4J_PASSWORD");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password, StandardCharsets.UTF_8);

        ObjectNode body = json.createObjectNode();
        body.put("statement", statement);
        body.set("parameters", json.valueToTree(parameters));

        JsonNode response = http.postForObject(
                endpoint, new HttpEntity<>(body, headers), JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Neo4j 返回空响应");
        }

        JsonNode errors = response.path("errors");
        if (errors.isArray() && errors.size() > 0) {
            throw new IllegalStateException(
                    "Neo4j 查询失败：" + errors.path(0).path("code").asText());
        }

        JsonNode data = response.path("data");
        if (!data.path("values").isArray()) {
            throw new IllegalStateException("Neo4j 未返回预期的查询结果");
        }
        return data;
    }
}
