package com.ktg.mes;

import com.ktg.mes.ai.controller.MesAiController;
import com.ktg.common.core.domain.AjaxResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.io.ByteArrayOutputStream;
import org.junit.Assert;

public class AiAnalysisControllerTest {
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void analysisUsesSuppliedFactsAndReturnsModelText() throws Exception {
        AtomicReference<String> sent = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/model", exchange -> {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024]; int len;
            while ((len = exchange.getRequestBody().read(chunk)) != -1) buf.write(chunk, 0, len);
            sent.set(new String(buf.toByteArray(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"模型分析结果\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            MesAiController controller = new MesAiController();
            set(controller, "deepseekKey", "test");
            set(controller, "deepseekUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/model");
            set(controller, "deepseekModel", "test-model");
            set(controller, "timeoutMs", 1000);
            Map<String, String> input = new HashMap<String, String>();
            input.put("question", "库存情况"); input.put("facts", "库存为5个");
            AjaxResult result = controller.analysis(input);
            Assert.assertEquals(200, result.get("code"));
            Assert.assertTrue(sent.get().contains("库存为5个"));
            Assert.assertEquals("模型分析结果", ((Map<?, ?>) result.get("data")).get("text"));
            input.put("facts", new String(new char[12001]).replace('\0', 'x'));
            Assert.assertEquals(500, controller.analysis(input).get("code"));
        } finally { server.stop(0); }
    }
}
