package com.ktg.mes.ai.controller;

import com.ktg.common.constant.HttpStatus;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.exception.ServiceException;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.service.AiBusinessAnswer;
import com.ktg.mes.ai.service.AiDifyWorkflow;
import com.ktg.mes.ai.service.AiReadTools;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mes/ai/business")
@PreAuthorize("@ss.hasPermi('mes:ai:query')")
public class MesAiBusinessController {
    private final AiDifyWorkflow workflow;
    private final AiBusinessAnswer business;
    private final AiReadTools tools;
    private final PermissionService permissions;

    public MesAiBusinessController(AiDifyWorkflow workflow, AiBusinessAnswer business, AiReadTools tools,
                                   PermissionService permissions) {
        this.workflow = workflow;
        this.business = business;
        this.tools = tools;
        this.permissions = permissions;
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
            workflow.answer(question, body.isAllowExternal(), body.isSkipLocal()), body.isSkipLocal()));
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
}
