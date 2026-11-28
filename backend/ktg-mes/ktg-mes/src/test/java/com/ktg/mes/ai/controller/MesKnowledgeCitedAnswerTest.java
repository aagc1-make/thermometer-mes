package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ktg.mes.ai.service.AiDifyWorkflow;
import com.ktg.mes.ai.service.KbRagEngines;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MesKnowledgeCitedAnswerTest {
    private final ObjectMapper json = new ObjectMapper();
    private JdbcTemplate jdbc;
    private MesKnowledgeGraphStore graphStore;
    private MesKnowledgeQuestionService service;
    private KbRagEngines engines;
    private MockRestServiceServer server;

    @Before
    public void setUp() {
        jdbc = mock(JdbcTemplate.class);
        Map<String, Object> version = new HashMap<>();
        version.put("version_id", 9L);
        version.put("document_id", 7L);
        version.put("original_name", "来料检验规范.txt");
        version.put("version_no", 1);
        when(jdbc.queryForList(anyString(), eq(1L))).thenReturn(singletonList(version));

        graphStore = mock(MesKnowledgeGraphStore.class);
        engines = mock(KbRagEngines.class);
        when(engines.citedDeepSeekText(any(JsonNode.class))).thenReturn("");
        service = new MesKnowledgeQuestionService(jdbc, json, graphStore, engines,
            new AiDifyWorkflow(null, engines, "", "", "12000"), "qwen2.5:3b");
        RestTemplate http = (RestTemplate) ReflectionTestUtils.getField(service, "http");
        server = MockRestServiceServer.bindTo(http).build();

        ObjectNode embedding = json.createObjectNode();
        ArrayNode vector = embedding.putArray("embeddings").addArray();
        for (int i = 0; i < 1024; i++) {
            vector.add(0);
        }
        server.expect(requestTo("http://127.0.0.1:11434/api/embed"))
                .andRespond(withSuccess(embedding.toString(), MediaType.APPLICATION_JSON));

        ObjectNode search = json.createObjectNode();
        search.putObject("result").putArray("points").addObject().putObject("payload")
                .put("owner_id", 1).put("version_id", 9).put("chunk_no", 1)
                .put("text", "来料检验项目包括响应时间和输出稳定性。");
        server.expect(requestTo("http://127.0.0.1:6333/collections/mes_knowledge/points/query"))
                .andRespond(withSuccess(search.toString(), MediaType.APPLICATION_JSON));
    }

    @Test
    public void usesDeepSeekByDefaultAndValidatesItsCitations() {
        when(engines.citedDeepSeekText(any(JsonNode.class)))
            .thenReturn("{\"answer\":\"应检验响应时间。\",\"sourceNumbers\":[1]}");

        JsonNode response = ask();
        assertEquals("应检验响应时间。[1]", response.path("answer").asText());
        assertEquals("DeepSeek", response.path("trace").path(1).path("detail").asText());
        server.verify();
    }

    @Test
    public void fallsBackLocallyWhenDeepSeekReturnsAnInvalidCitation() {
        when(engines.citedDeepSeekText(any(JsonNode.class)))
            .thenReturn("{\"answer\":\"应检验响应时间。\",\"sourceNumbers\":[99]}");
        expectAnswer("{\"answer\":\"应检验输出稳定性。\",\"sourceNumbers\":[1]}");

        assertEquals("应检验输出稳定性。[1]", ask().path("answer").asText());
        server.verify();
    }

    @Test
    public void explicitlyLocalQuestionDoesNotCallDeepSeek() {
        expectAnswer("{\"answer\":\"应检验响应时间。\",\"sourceNumbers\":[1]}");

        assertEquals("应检验响应时间。[1]",
            service.ask(1, "来料要检验哪些项目？", "rag", false).path("answer").asText());
        verifyNoInteractions(engines);
        server.verify();
    }

    @Test
    public void rendersValidatedCitationsAndDeduplicatesThem() {
        expectAnswer("{\"answer\":\"应检验响应时间和输出稳定性。[99]\",\"sourceNumbers\":[1,1]}");

        JsonNode result = ask();

        assertEquals("应检验响应时间和输出稳定性。[1]", result.path("answer").asText());
        assertEquals(1, result.path("sources").path(0).path("number").asInt());
        server.verify();
    }

    @Test
    public void retriesWhenModelReturnsOnlyAnInlineReference() {
        expectAnswer("{\"answer\":\"[1]\",\"sourceNumbers\":[1]}");
        expectAnswer("{\"answer\":\"应检验响应时间。\",\"sourceNumbers\":[1]}");

        assertEquals("应检验响应时间。[1]", ask().path("answer").asText());
        server.verify();
    }

    @Test
    public void rejectsCitationsThatAreNotInTheResponseSources() {
        expectAnswer("{\"answer\":\"应检查外观。\",\"sourceNumbers\":[99]}");
        expectAnswer("{\"answer\":\"应检查外观。\",\"sourceNumbers\":[]}");

        assertThrows(MesKnowledgeQuestionService.AnswerCitationException.class, this::ask);
        server.verify();
    }

    @Test
    public void keepsTheNoEvidenceAnswerWithoutCitations() {
        expectAnswer("{\"answer\":\"知识库中未找到依据。\",\"sourceNumbers\":[1]}");

        JsonNode result = ask();

        assertEquals("知识库中未找到依据", result.path("answer").asText());
        assertEquals(0, result.path("sources").size());
        server.verify();
    }

    @Test
    public void sendsEachSourceOnceAndReusesTheCitationAcrossGraphPaths() {
        ObjectNode graphResult = json.createObjectNode();
        ArrayNode rows = graphResult.putArray("values");
        ArrayNode responseTimePath = rows.addArray().addArray();
        responseTimePath.add(inspectionFact("响应时间", 7));
        rows.addArray().add(responseTimePath.deepCopy());
        rows.addArray().addArray().add(inspectionFact("输出稳定性", 7));
        rows.addArray().addArray().add(inspectionFact("尺寸", 7)).add(inspectionFact("外观", 999));
        when(graphStore.query(anyString(), anyMap())).thenReturn(graphResult);

        expectAnswer("{\"answer\":\"应检验响应时间和输出稳定性。\",\"sourceNumbers\":[1]}",
                "问题：\n来料要检验哪些项目？\n\n资料：\n"
                        + "[1] 来料检验项目包括响应时间和输出稳定性。\n"
                        + "\n图谱关系（均来自已编号资料）\n"
                        + "红外传感器 — 需要检验 → 响应时间 [1]\n"
                        + "红外传感器 — 需要检验 → 输出稳定性 [1]\n");

        JsonNode result = service.ask(1, "来料要检验哪些项目？", "graph_rag");

        assertEquals("matched", result.path("graphStatus").asText());
        assertEquals(1, result.path("sources").size());
        assertEquals(3, result.path("paths").size());
        for (JsonNode path : result.path("paths")) {
            assertEquals(1, path.path(0).path("sourceNumber").asInt());
        }
        server.verify();
    }

    @Test
    public void returnsGraphNoMatchIfThereAreNoActiveVersions() {
        when(jdbc.queryForList(anyString(), eq(1L))).thenReturn(emptyList());
        server.reset();

        JsonNode result = service.ask(1, "来料要检验哪些项目？", "graph_rag");

        assertEquals("no_match", result.path("graphStatus").asText());
        assertEquals(0, result.path("paths").size());
        server.verify();
    }

    @Test
    public void reportsNeo4jConnectionFailureSeparately() {
        when(graphStore.query(anyString(), anyMap()))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThrows(MesKnowledgeQuestionService.GraphQueryException.class,
                () -> service.ask(1, "来料要检验哪些项目？", "graph_rag"));
        server.verify();
    }

    private ObjectNode inspectionFact(String item, long documentId) {
        return json.createObjectNode()
                .put("subject", "红外传感器").put("predicate", "需要检验").put("object", item)
                .put("documentId", documentId).put("versionId", 9).put("chunkNo", 1)
                .put("evidence", "来料检验项目包括响应时间和输出稳定性。");
    }

    private JsonNode ask() {
        return service.ask(1, "来料要检验哪些项目？", "rag");
    }

    private void expectAnswer(String content) {
        expectAnswer(content, null);
    }

    private void expectAnswer(String content, String expectedContext) {
        ObjectNode reply = json.createObjectNode();
        reply.putObject("message").put("content", content);
        ResponseActions request = server.expect(requestTo("http://127.0.0.1:11434/api/chat"))
                .andExpect(jsonPath("$.format.properties.sourceNumbers.items.type").value("integer"));
        if (expectedContext != null) {
            request.andExpect(jsonPath("$.messages[1].content").value(expectedContext));
        }
        request.andRespond(withSuccess(reply.toString(), MediaType.APPLICATION_JSON));
    }
}
