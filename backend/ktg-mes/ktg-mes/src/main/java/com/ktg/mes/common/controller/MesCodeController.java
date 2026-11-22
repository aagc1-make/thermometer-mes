package com.ktg.mes.common.controller;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.system.service.CodeSequenceService;

/**
 * 单据取号预览：新增弹窗打开时先取一个号用于展示，保存时原样回传。
 * 仅开放业务单据规则，避免前端任意消耗流水。
 */
@RestController
@RequestMapping("/mes/code")
public class MesCodeController extends BaseController
{
    private static final Set<String> ALLOWED_RULES = new HashSet<>(Arrays.asList(
            "DHTZ", "LL", "TL", "CTH", "WXLL", "WXRK", "CPRK", "XSCK", "XSTH",
            "FHTZ", "YKD", "PD", "PDJH", "BG", "ZJ", "IQC", "IPQC", "OQC", "RQC",
            "JYYP", "RW", "LZK", "SCK", "WXD", "ZCL", "ZCR", "BZ", "GD"));

    @Autowired
    private CodeSequenceService codeSequenceService;

    @GetMapping("/next/{ruleCode}")
    public AjaxResult next(@PathVariable String ruleCode,
                           @RequestParam(required = false) String referenceCode)
    {
        String rule = ruleCode == null ? "" : ruleCode.trim().toUpperCase();
        if (!ALLOWED_RULES.contains(rule)) {
            return AjaxResult.error("不支持的编码规则：" + ruleCode);
        }
        // 传参照单号时取链号（日期段与流水完全沿用订单/工单号），保证同一业务链单号严格对应
        String code = referenceCode == null || referenceCode.trim().isEmpty()
                ? codeSequenceService.generateCode(rule)
                : codeSequenceService.generateChainCode(rule, referenceCode.trim());
        return AjaxResult.success("操作成功", code);
    }
}
