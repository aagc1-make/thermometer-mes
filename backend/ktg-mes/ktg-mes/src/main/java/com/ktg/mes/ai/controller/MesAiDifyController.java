package com.ktg.mes.ai.controller;

import com.ktg.mes.ai.service.AiDifyWorkflow;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dify 工作流回调入口。
 *
 * <p>请求只携带单步凭证与固定阶段名；响应体为下一步凭证（最后一步为完成回执），MES 的引用、表格、
 * 证据与回答正文都不经过这里。凭证不可重复使用，阶段不符或过期直接 403。
 */
@RestController
public class MesAiDifyController {
    private final AiDifyWorkflow workflow;

    public MesAiDifyController(AiDifyWorkflow workflow) {
        this.workflow = workflow;
    }

    @PostMapping(value = "/mes/ai/dify/callback", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> callback(@RequestBody(required = false) Callback body) {
        if (body == null) {
            return ResponseEntity.badRequest().body("bad_request");
        }
        AiDifyWorkflow.Ack ack = workflow.callback(body.getTicket(), body.getStage());
        ResponseEntity.BodyBuilder builder = ack.accepted()
            ? ResponseEntity.ok() : ResponseEntity.status(HttpStatus.FORBIDDEN);
        return builder
            .contentType(MediaType.TEXT_PLAIN)
            .header("X-MES-Stage", ack.stage())
            .header("X-MES-Next-Stage", ack.nextStage())
            .header("X-MES-Route", ack.route())
            .body(ack.accepted() ? ack.body() : "rejected");
    }

    @Data
    public static class Callback {
        private String ticket;
        private String stage;
    }
}
