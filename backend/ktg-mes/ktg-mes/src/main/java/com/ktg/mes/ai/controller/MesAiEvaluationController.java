package com.ktg.mes.ai.controller;

import com.ktg.common.constant.HttpStatus;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.ai.service.AiEvaluationService;
import com.ktg.mes.ai.service.KbDocumentService;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mes/ai/evaluation")
@PreAuthorize("@ss.hasRole('admin') and @ss.hasPermi('mes:ai:query')")
public class MesAiEvaluationController {
    private final KbDocumentService libraries;
    private final AiEvaluationService evaluation;

    public MesAiEvaluationController(KbDocumentService libraries, AiEvaluationService evaluation) {
        this.libraries = libraries;
        this.evaluation = evaluation;
    }

    @PostMapping("/run")
    public AjaxResult run(@RequestBody Request request) {
        if (request.isSuite()) return AjaxResult.success(evaluation.runSuite());
        if (request.getLibraryId() == null || request.getLibraryId() <= 0) {
            throw new ServiceException("请选择部门知识库。", HttpStatus.BAD_REQUEST);
        }
        return AjaxResult.success(evaluation.run(libraries.readScope(request.getLibraryId())));
    }

    @GetMapping("/suite")
    public AjaxResult suite() {
        return AjaxResult.success(evaluation.suiteAvailable());
    }

    @Data
    public static class Request {
        private Long libraryId;
        private boolean suite;
    }
}
