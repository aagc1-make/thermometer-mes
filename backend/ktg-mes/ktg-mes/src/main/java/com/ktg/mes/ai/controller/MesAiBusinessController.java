package com.ktg.mes.ai.controller;

import com.ktg.common.constant.HttpStatus;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.exception.ServiceException;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.service.AiBusinessAnswer;
import com.ktg.mes.ai.service.AiDifyWorkflow;
import com.ktg.mes.ai.service.AiReadTools;
import com.ktg.mes.ai.service.KbRagEngines;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mes/ai/business")
@PreAuthorize("@ss.hasPermi('mes:ai:query')")
public class MesAiBusinessController {
    private final AiDifyWorkflow workflow;
    private final AiBusinessAnswer business;
    private final AiReadTools tools;
    private final PermissionService permissions;
    private final KbRagEngines engines;

    public MesAiBusinessController(AiDifyWorkflow workflow, AiBusinessAnswer business, AiReadTools tools,
                                   PermissionService permissions, KbRagEngines engines) {
        this.workflow = workflow;
        this.business = business;
        this.tools = tools;
        this.permissions = permissions;
        this.engines = engines;
    }

    @GetMapping("/tools")
    @PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('mes:ai:query')")
    public AjaxResult tools() {
        return AjaxResult.success(tools.manifest());
    }

    @PostMapping("/answer")
    public AjaxResult answer(@RequestBody Question body) {
        String question = body.getQuestion() == null ? "" : body.getQuestion().trim();
        if (question.isEmpty() || question.length() > 600) {
            throw new ServiceException("请输入 1 至 600 字的问题。", HttpStatus.BAD_REQUEST);
        }
        if (body.isSkipLocal() && !permissions.hasRole("admin")) {
            throw new ServiceException("只有管理员可以模拟本地模型离线。", HttpStatus.FORBIDDEN);
        }
        return AjaxResult.success(business.addAnswer(question,
            workflow.answer(question, body.isAllowExternal(), body.isSkipLocal()), body.isAllowExternal(), body.isSkipLocal()));
    }

    /**
     * 规则都匹配不上时的兜底：先让模型尝试解析成只读工具调用，取不到数据再由模型自然作答，
     * 避免用户看到“我没有识别出这个问题”这类固定话术。
     */
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody Question body) {
        String question = body.getQuestion() == null ? "" : body.getQuestion().trim();
        if (question.isEmpty() || question.length() > 600) {
            throw new ServiceException("请输入 1 至 600 字的问题。", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> answer = workflow.answer(question, body.isAllowExternal(), body.isSkipLocal());
        Object results = answer == null ? null : answer.get("results");
        if (results instanceof List && !((List<?>) results).isEmpty()) {
            return AjaxResult.success(business.addAnswer(question, answer, body.isAllowExternal(), body.isSkipLocal()));
        }
        String text = engines.naturalText(body.isAllowExternal(), KbRagEngines.ASSISTANT_SYSTEM,
            "用户问题：" + question + "\n\n说明：本轮没有取得业务数据，也没有可引用的授权文档。"
                + "问候、闲聊或 MES 概念问题请直接自然回答；确实需要数据时说明暂未查到，并提示用户换成更具体的问法。");
        if (text.isEmpty()) {
            return AjaxResult.error("模型暂不可用");
        }
        Map<String, Object> chat = new LinkedHashMap<String, Object>();
        chat.put("mode", "chat");
        chat.put("answer", text);
        chat.put("results", Collections.emptyList());
        chat.put("trace", Collections.emptyList());
        return AjaxResult.success(chat);
    }

    /** 把规则命中的表格或清单改写为自然语言；模型不可用时由前端保留原句。 */
    @PostMapping("/narrate")
    public AjaxResult narrate(@RequestBody Narration body) {
        String question = body.getQuestion() == null ? "" : body.getQuestion().trim();
        String facts = body.getFacts() == null ? "" : body.getFacts().trim();
        if (question.isEmpty() || facts.isEmpty()) {
            throw new ServiceException("问题和结果不能为空。", HttpStatus.BAD_REQUEST);
        }
        if (question.length() > 1000 || facts.length() > 20000) {
            throw new ServiceException("内容过长，请缩小查询范围。", HttpStatus.BAD_REQUEST);
        }
        String text = business.narrate(body.isAllowExternal(), question, facts);
        if (text.isEmpty()) {
            return AjaxResult.error("模型暂不可用");
        }
        return AjaxResult.success(Collections.singletonMap("text", text));
    }

    @PostMapping("/purchase-receipts")
    @PreAuthorize("@ss.hasPermi('mes:ai:query') and @ss.hasPermi('mes:wm:transaction:list')")
    public AjaxResult purchaseReceipts(@RequestBody ReceiptQuery body) {
        return AjaxResult.success(tools.purchaseReceipts(body.getItemCode(), body.getFrom(), body.getTo(), body.getPage()));
    }

    @Data
    public static class Question {
        private String question;
        private boolean allowExternal;
        private boolean skipLocal;
    }

    @Data
    public static class ReceiptQuery {
        private String itemCode;
        private String from;
        private String to;
        // A missing page means count only.
        private Integer page;
    }

    @Data
    public static class Narration {
        private String question;
        private String facts;
        private boolean allowExternal;
    }
}
