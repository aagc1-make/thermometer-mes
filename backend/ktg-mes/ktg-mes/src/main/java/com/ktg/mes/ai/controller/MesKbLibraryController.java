package com.ktg.mes.ai.controller;

import com.ktg.common.annotation.Log;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.SecurityUtils;
import com.ktg.mes.ai.service.KbLibraryService;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mes/ai/kb/libraries")
public class MesKbLibraryController {
    private final KbLibraryService libraryService;

    public MesKbLibraryController(KbLibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public AjaxResult list(@RequestParam(value = "deptId", required = false) String requestedDeptId) {
        if (requestedDeptId != null) {
            throw new ServiceException("只能查看当前账号所属部门的知识库", HttpStatus.FORBIDDEN);
        }
        return AjaxResult.success(libraryService.listVisible(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/admin")
    public AjaxResult listAdminSummaries() {
        return AjaxResult.success(libraryService.listAdminSummaries());
    }

    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "部门知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody CreateLibraryRequest request) {
        libraryService.create(request.getDeptId(), request.getName());
        return AjaxResult.success();
    }

    @Data
    public static class CreateLibraryRequest {
        private Long deptId;
        private String name;
    }
}
