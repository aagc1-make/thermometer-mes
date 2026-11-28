package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MesKnowledgeGraphIndexerTest {

    private static final String CHAT = "http://127.0.0.1:11434/api/chat";
    private static final String INSTALL_FRAGMENT = "电子温度计在组装工序安装温度探头。";
    private static final String VALID_INSTALL_FACT =
            "{\"subjectType\":\"工序\",\"subject\":\"组装工序\",\"predicate\":\"安装\","
                    + "\"objectType\":\"部件\",\"object\":\"温度探头\","
                    + "\"evidence\":\"" + INSTALL_FRAGMENT + "\"}";

    private final ObjectMapper json = new ObjectMapper();
    private MesKnowledgeGraphIndexer indexer;
    private MockRestServiceServer server;

    @Before
    public void setUp() {
        indexer = new MesKnowledgeGraphIndexer(mock(MesKnowledgeGraphStore.class), json, "test-model");
        RestTemplate ollama = (RestTemplate) ReflectionTestUtils.getField(indexer, "ollama");
        server = MockRestServiceServer.bindTo(ollama).build();
    }

    @Test
    public void skipsFactsThatAreNotGroundedInTheFragment() {
        expectModel("{\"facts\":[" + VALID_INSTALL_FACT + ","
                + "{\"subjectType\":\"部件\",\"subject\":\"温度探头\",\"predicate\":\"需要检验\","
                + "\"objectType\":\"检验项\",\"object\":\"外观\","
                + "\"evidence\":\"" + INSTALL_FRAGMENT + "\"}]}");

        List<Map<String, Object>> rows = rows(prepare(INSTALL_FRAGMENT));

        assertEquals(1, rows.size());
        assertEquals("组装工序", rows.get(0).get("subject"));
        assertEquals("温度探头", rows.get(0).get("object"));
        server.verify();
    }

    @Test
    public void skipsFragmentsWhoseExtractionShapeIsUnusable() {
        expectModel("{\"facts\":{\"AQL\":\"接收质量限\"}}");

        List<Map<String, Object>> rows = rows(prepare("3.1　AQL：接收质量限。"));

        assertEquals(0, rows.size());
        server.verify();
    }

    @Test
    public void keepsExtractingAfterAnUnusableFragment() {
        expectModel("无法返回 JSON");
        expectModel("{\"facts\":[" + VALID_INSTALL_FACT + "]}");

        List<Map<String, Object>> rows = rows(prepare("3.1　AQL：接收质量限。", INSTALL_FRAGMENT));

        assertEquals(1, rows.size());
        assertEquals("组装工序", rows.get(0).get("subject"));
        server.verify();
    }

    @Test
    public void keepsOllamaOutagesFatal() {
        server.expect(requestTo(CHAT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(RestClientException.class,
                () -> prepare(INSTALL_FRAGMENT));
    }

    private Map<String, Object> prepare(String... texts) {
        List<MesKnowledgeTextExtractor.Section> sections = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            sections.add(new MesKnowledgeTextExtractor.Section(texts[i], "正文段落 " + (i + 1)));
        }
        return indexer.prepareGraph(1L, 2L, 3L, MesKnowledgeChunks.split(sections));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> parameters) {
        return (List<Map<String, Object>>) parameters.get("rows");
    }

    private void expectModel(String content) {
        ObjectNode reply = json.createObjectNode();
        reply.putObject("message").put("content", content);
        server.expect(requestTo(CHAT))
                .andRespond(withSuccess(reply.toString(), MediaType.APPLICATION_JSON));
    }
}
