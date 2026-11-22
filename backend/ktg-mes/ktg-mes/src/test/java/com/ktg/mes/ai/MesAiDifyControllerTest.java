package com.ktg.mes.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktg.mes.ai.controller.MesAiDifyController;
import com.ktg.mes.ai.service.AiDifyWorkflow;
import com.ktg.mes.ai.service.KbRagEngines;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MesAiDifyControllerTest {
    private final MesAiDifyController controller = new MesAiDifyController(new AiDifyWorkflow(null,
        new KbRagEngines(new ObjectMapper()), "http://dify.local/v1", "app-test-key", "5000"));

    private static MesAiDifyController.Callback callback(String ticket, String stage) {
        MesAiDifyController.Callback body = new MesAiDifyController.Callback();
        body.setTicket(ticket);
        body.setStage(stage);
        return body;
    }

    private static String ticket(char fill) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 43; i++) {
            builder.append(fill);
        }
        return builder.toString();
    }

    @Test
    public void unknownTicketIsRejectedWithoutLeakingDetails() {
        ResponseEntity<String> response = controller.callback(callback(ticket('A'), "rewrite"));

        assertEquals(403, response.getStatusCodeValue());
        assertEquals("rejected", response.getBody());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertEquals("rewrite", response.getHeaders().getFirst("X-MES-Stage"));
        assertEquals("", response.getHeaders().getFirst("X-MES-Next-Stage"));
        assertEquals("", response.getHeaders().getFirst("X-MES-Route"));
    }

    @Test
    public void malformedTicketIsRejected() {
        ResponseEntity<String> response = controller.callback(callback("short", "rewrite"));

        assertEquals(403, response.getStatusCodeValue());
        assertEquals("rejected", response.getBody());
    }

    @Test
    public void emptyBodyIsRejected() {
        ResponseEntity<String> response = controller.callback(null);

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("bad_request"));
    }
}
