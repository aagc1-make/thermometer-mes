package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MesKnowledgeGraphStoreTest {

    @Test
    public void returnsOneFromLocalNeo4j() {
        assumeTrue(Boolean.getBoolean("mes.neo4j.smoke"));

        String password = System.getenv("MES_NEO4J_PASSWORD");
        assertTrue("请在运行 Maven 的终端设置 MES_NEO4J_PASSWORD",
                password != null && !password.isEmpty());

        MesKnowledgeGraphStore store = new MesKnowledgeGraphStore(
                new ObjectMapper(), "http://127.0.0.1:7474",
                "neo4j", "neo4j", password);

        JsonNode data = store.query(
                "RETURN $value AS ok",
                Collections.singletonMap("value", 1));

        assertEquals("ok", data.path("fields").path(0).asText());
        assertEquals(1, data.path("values").path(0).path(0).asInt(-1));
    }
}