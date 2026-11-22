package com.ktg.mes.wm.controller;

import com.ktg.common.core.domain.AjaxResult;
import com.ktg.mes.wm.service.IWmBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批次只读追溯（独立路径，避免与 /mes/wm/batch/{batchId} 冲突）
 */
@RestController
@RequestMapping("/mes/wm/batchTrace")
public class WmBatchTraceController {

    @Autowired
    private IWmBatchService wmBatchService;

    /** 成品批次 → 产品入库/工单/领料明细(原料批次)/库存流水/销售（全部只读） */
    @PreAuthorize("@ss.hasPermi('mes:wm:batch:query')")
    @GetMapping("/{batchCode}")
    public AjaxResult trace(@PathVariable("batchCode") String batchCode) {
        return AjaxResult.success(wmBatchService.traceBatch(batchCode));
    }
}
