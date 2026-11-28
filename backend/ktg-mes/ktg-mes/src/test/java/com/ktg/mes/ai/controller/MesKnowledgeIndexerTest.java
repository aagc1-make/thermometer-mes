package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MesKnowledgeIndexerTest {

    private static final String COLLECTION = "http://127.0.0.1:6333/collections/mes_knowledge";
    private static final String POINTS = COLLECTION + "/points?wait=true";
    private static final String EMBED = "http://127.0.0.1:11434/api/embed";

    private final ObjectMapper json = new ObjectMapper();
    private MesKnowledgeIndexer indexer;
    private MockRestServiceServer server;

    @Before
    public void setUp() {
        indexer = new MesKnowledgeIndexer();
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(indexer, "http");
        server = MockRestServiceServer.bindTo(http).build();
    }

    @Test
    public void createsMissingCollectionBeforeWritingVectors() {
        server.expect(requestTo(COLLECTION)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(COLLECTION)).andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"result\":true,\"status\":\"ok\"}", MediaType.APPLICATION_JSON));
        expectEmbedding();
        expectUpsert();

        indexer.index(1L, 2L, 3L, chunks("电子温度计在组装工序安装温度探头。"));

        server.verify();
    }

    @Test
    public void reusesExistingCollectionWithoutRecreatingIt() {
        server.expect(requestTo(COLLECTION)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"result\":{},\"status\":\"ok\"}", MediaType.APPLICATION_JSON));
        expectEmbedding();
        expectUpsert();

        indexer.index(1L, 2L, 3L, chunks("电子温度计在组装工序安装温度探头。"));

        server.verify();
    }

    @Test
    public void toleratesConcurrentCollectionCreation() {
        server.expect(requestTo(COLLECTION)).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(COLLECTION)).andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        expectEmbedding();
        expectUpsert();

        indexer.index(1L, 2L, 3L, chunks("电子温度计在组装工序安装温度探头。"));

        server.verify();
    }

    private void expectEmbedding() {
        ObjectNode embedding = json.createObjectNode();
        ArrayNode vector = embedding.putArray("embeddings").addArray();
        for (int i = 0; i < 1024; i++) {
            vector.add(0);
        }
        server.expect(requestTo(EMBED)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(embedding.toString(), MediaType.APPLICATION_JSON));
    }

    private void expectUpsert() {
        server.expect(requestTo(POINTS)).andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(
                        "{\"result\":{\"status\":\"completed\"},\"status\":\"ok\"}",
                        MediaType.APPLICATION_JSON));
    }

    private java.util.List<MesKnowledgeChunks.Chunk> chunks(String text) {
        return MesKnowledgeChunks.split(Collections.singletonList(
                new MesKnowledgeTextExtractor.Section(text, "正文段落 1")));
    }
}
