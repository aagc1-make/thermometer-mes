package com.ktg.mes.pro.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;

/**
 * 贯通单据清理：查询候选工单、预览级联删除范围、执行删除。
 *
 * 用于清理“全流程贯通”产生的工单及其关联单据，
 * 保留主数据（物料、BOM、工艺路线、仓库、客户、供应商、检验方案）。
 */
@RestController
@RequestMapping("/mes/pro/chain")
public class ChainCleanupController extends BaseController {

    @Autowired
    private ChainCleanupService chainCleanupService;

    /** 候选工单（带各单据数量），onlyChain=true 时只返回贯通标记工单。 */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @GetMapping("/candidates")
    public AjaxResult candidates(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false, defaultValue = "false") boolean onlyChain) {
        return AjaxResult.success(chainCleanupService.candidates(keyword, onlyChain));
    }

    /** 预览：展开子工单后的删除行数、库存影响与风险提示（只读）。 */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIds(body.get("workorderIds"));
        if (ids.isEmpty()) {
            return AjaxResult.error("请先选择要清理的工单");
        }
        return AjaxResult.success(chainCleanupService.preview(ids));
    }

    /** 执行清理：单事务级联删除，库存按流水反冲。 */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @Log(title = "贯通单据清理", businessType = BusinessType.DELETE)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIds(body.get("workorderIds"));
        if (ids.isEmpty()) {
            return AjaxResult.error("请先选择要清理的工单");
        }
        Object confirm = body.get("confirmText");
        try {
            Map<String, Object> result = chainCleanupService.execute(ids, confirm == null ? null : String.valueOf(confirm));
            result.put("operator", getUsername());
            return AjaxResult.success(result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error("贯通单据清理失败", e);
            return AjaxResult.error("清理失败：" + e.getMessage());
        }
    }

    private List<Long> toIds(Object value) {
        List<Long> ids = new ArrayList<Long>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item == null) {
                    continue;
                }
                try {
                    ids.add(Long.valueOf(String.valueOf(item)));
                } catch (NumberFormatException ignored) {
                    // 忽略非数字项
                }
            }
        } else if (value != null) {
            for (String item : String.valueOf(value).split(",")) {
                if (item.trim().isEmpty()) {
                    continue;
                }
                try {
                    ids.add(Long.valueOf(item.trim()));
                } catch (NumberFormatException ignored) {
                    // 忽略非数字项
                }
            }
        }
        return ids;
    }
}
