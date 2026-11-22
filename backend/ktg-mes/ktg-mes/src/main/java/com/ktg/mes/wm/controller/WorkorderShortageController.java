package com.ktg.mes.wm.controller;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan.Confirmation;
import com.ktg.mes.wm.service.impl.WorkorderShortageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mes/pro/workorder/{id}/shortage")
public class WorkorderShortageController extends BaseController {
    private static final String READ = "@ss.hasPermi('mes:pro:workorder:query') and @ss.hasPermi('mes:wm:wmstock:list')"
            + " and @ss.hasPermi('mes:wm:issueheader:list') and @ss.hasPermi('mes:wm:transfer:list')";
    private final WorkorderShortageService service;

    public WorkorderShortageController(WorkorderShortageService service) { this.service = service; }

    @GetMapping
    @PreAuthorize(READ)
    public AjaxResult analyze(@PathVariable("id") long id) {
        return AjaxResult.success(service.analyze(id));
    }

    @PostMapping("/draft")
    @PreAuthorize(READ + " and @ss.hasPermi('mes:wm:transfer:add')")
    @Log(title = "工单缺料调拨草稿", businessType = BusinessType.INSERT)
    public AjaxResult confirm(@PathVariable("id") long id, @RequestBody Confirmation request) {
        return AjaxResult.success(service.confirm(id, request, getUsername()));
    }
}
