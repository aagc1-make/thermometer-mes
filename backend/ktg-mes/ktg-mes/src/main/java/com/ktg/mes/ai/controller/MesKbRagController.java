package com.ktg.mes.ai.controller;

import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.service.KbDocumentService;
import com.ktg.mes.ai.service.KbRagService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class MesKbRagController {
    private static final Logger log = LoggerFactory.getLogger(MesKbRagController.class);
    private final KbDocumentService documents;
    private final KbRagService rag;
    private final PermissionService permissions;
    private final Executor executor;

    public MesKbRagController(KbDocumentService documents, KbRagService rag, PermissionService permissions,
                              @Qualifier("threadPoolTaskExecutor") Executor executor) {
        this.documents = documents;
        this.rag = rag;
        this.permissions = permissions;
        this.executor = executor;
    }

    @PreAuthorize("@ss.hasPermi('mes:ai:query')")
    @PostMapping("/mes/ai/kb/libraries/{libraryId}/answer")
    public SseEmitter answer(@PathVariable("libraryId") long libraryId, @RequestBody Question body) {
        KbDocument.Library scope = documents.readScope(libraryId);
        String question = body.getQuestion() == null ? "" : body.getQuestion().trim();
        if (question.isEmpty() || question.length() > 600) {
            throw new ServiceException("请输入 1 至 600 字的问题", HttpStatus.BAD_REQUEST);
        }
        if (body.isSkipLocal() && !permissions.hasRole("admin")) {
            throw new ServiceException("只有管理员可以模拟本地模型离线", HttpStatus.FORBIDDEN);
        }
        SseEmitter emitter = new SseEmitter(150000L);
        AtomicBoolean stopped = new AtomicBoolean();
        emitter.onCompletion(() -> stopped.set(true));
        emitter.onError(error -> stopped.set(true));
        emitter.onTimeout(() -> {
            stopped.set(true);
            emitter.complete();
        });
        // SSE 在独立线程执行，必须把本次请求的登录身份带过去：混合问答的业务分支要按该账号校验只读工具权限
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        executor.execute(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            if (caller != null) {
                context.setAuthentication(caller);
            }
            SecurityContextHolder.setContext(context);
            try {
                String businessQuestion = body.isBusiness() ? body.getBusinessQuestion() : null;
                rag.answer(scope, question, body.isAllowExternal(), body.isSkipLocal(), body.isGraph(),
                    businessQuestion, (event, value) -> {
                        if (stopped.get()) {
                            throw new IOException("Client disconnected");
                        }
                        try {
                            emitter.send(SseEmitter.event().name(event)
                                .data(Collections.singletonMap("value", value), MediaType.APPLICATION_JSON));
                        } catch (IOException | IllegalStateException e) {
                            stopped.set(true);
                            throw new IOException("Client disconnected", e);
                        }
                    }, stopped::get);
            } catch (IOException | RuntimeException e) {
                if (!stopped.get()) {
                    log.warn("部门库问答未完成，库 {}", libraryId, e);
                    try {
                        emitter.send(SseEmitter.event().name("error")
                            .data(Collections.singletonMap("value", "问答未完成，请重试。"), MediaType.APPLICATION_JSON));
                    } catch (IOException | IllegalStateException ignored) {
                        stopped.set(true);
                    }
                }
            } finally {
                SecurityContextHolder.setContext(previous);
                emitter.complete();
            }
        });
        return emitter;
    }

    @Data
    public static class Question {
        private String question;
        private boolean allowExternal;
        private boolean skipLocal;
        private boolean graph;
        /** 同一轮混合问答：为 true 时文档与业务两路检索共用一个 Dify 运行编号。 */
        private boolean business;
        /** MES 侧已拆分的业务子问题，只用于选择只读工具参数。 */
        private String businessQuestion;
    }
}
