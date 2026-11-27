package com.ktg.system.domain;

/**
 * 编码规则 sys_code_rule
 *
 * <p>对应工单主链 v2 的编码规则表：前缀 + 日期段 + 定长流水。</p>
 *
 * @author ktg
 */
public class SysCodeRule
{
    /** 规则编码，如 GD/PCGD/RKZJGD */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 单号前缀，如 gd/pcgd */
    private String prefix;

    /** 日期段格式，如 yyyyMMdd；空表示无日期段 */
    private String dateFormat;

    /** 流水段定长 */
    private Integer serialLength;

    /** 起始流水号 */
    private Long serialStart;

    /** 流水步长 */
    private Integer serialStep;

    /** 流水重置周期 NONE/YEAR/MONTH/DAY */
    private String resetCycle;

    /** 是否启用 Y/N */
    private String enableFlag;

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Integer getSerialLength() {
        return serialLength;
    }

    public void setSerialLength(Integer serialLength) {
        this.serialLength = serialLength;
    }

    public Long getSerialStart() {
        return serialStart;
    }

    public void setSerialStart(Long serialStart) {
        this.serialStart = serialStart;
    }

    public Integer getSerialStep() {
        return serialStep;
    }

    public void setSerialStep(Integer serialStep) {
        this.serialStep = serialStep;
    }

    public String getResetCycle() {
        return resetCycle;
    }

    public void setResetCycle(String resetCycle) {
        this.resetCycle = resetCycle;
    }

    public String getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag) {
        this.enableFlag = enableFlag;
    }
}
