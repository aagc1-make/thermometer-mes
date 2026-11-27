package com.ktg.system.service;

import com.ktg.system.domain.SysCodeRule;
import com.ktg.system.mapper.SysCodeSequenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务单号取号服务。
 *
 * <p>单号 = 2位小写拼音前缀 + yyyyMMdd + 5位流水，例如 gd2026111400001。</p>
 *
 * <p>保存时用数据库唯一键占号，预览只读。旧单号也参与占用检查。</p>
 *
 * @author ktg
 */
@Service
public class CodeSequenceService
{
    /** 规则编码 -> 业务表.单号列；质检类规则共享同一前缀，需要在多张表间统一取最小可用号。 */
    private static final Map<String, List<String[]>> CODE_TARGETS = new HashMap<>();

    static
    {
        bind("GD", "pro_workorder", "workorder_code");
        bind("XSDD", "wm_sales_order", "order_code");
        bind("RW", "pro_task", "task_code");
        bind("BG", "pro_feedback", "feedback_code");
        bind("LZK", "pro_card", "card_code");
        bind("SCK", "pro_card", "card_code");
        bind("PCGD", "wm_batch", "batch_code");
        bind("PCCG", "wm_batch", "batch_code");
        bind("PCWS", "wm_batch", "batch_code");
        bind("RKCG", "wm_item_recpt", "recpt_code");
        bind("DHTZ", "wm_arrival_notice", "notice_code");
        bind("LL", "wm_issue_header", "issue_code");
        bind("TL", "wm_rt_issue", "rt_code");
        bind("CTH", "wm_rt_vendor", "rt_code");
        bind("WXLL", "wm_outsource_issue", "issue_code");
        bind("WXRK", "wm_outsource_recpt", "recpt_code");
        bind("CPRK", "wm_product_recpt", "recpt_code");
        bind("XSCK", "wm_product_sales", "sales_code");
        bind("XSTH", "wm_rt_sales", "rt_code");
        bind("FHTZ", "wm_sales_notice", "notice_code");
        bind("YKD", "wm_transfer", "transfer_code");
        bind("PD", "wm_stock_taking", "taking_code");
        bind("PDJH", "wm_stock_taking_plan", "plan_code");
        bind("WXD", "dv_repair", "repair_code");
        bind("ZCL", "wm_misc_issue", "issue_code");
        bind("ZCR", "wm_misc_recpt", "recpt_code");
        bind("BZ", "wm_package", "package_code");
        bind("TXN", "wm_transaction", "txn_code");
        bind("JYYP", "qc_result", "result_code");
        bind("RKZJGD", "qc_inbound", "inspection_code");
        bind("ZJ", "qc_iqc", "iqc_code");
        bind("ZJ", "qc_ipqc", "ipqc_code");
        bind("ZJ", "qc_oqc", "oqc_code");
        bind("ZJ", "qc_rqc", "rqc_code");
        bind("IQC", "qc_iqc", "iqc_code");
        bind("IPQC", "qc_ipqc", "ipqc_code");
        bind("OQC", "qc_oqc", "oqc_code");
        bind("RQC", "qc_rqc", "rqc_code");
    }

    private static void bind(String ruleCode, String table, String column)
    {
        CODE_TARGETS.computeIfAbsent(ruleCode, k -> new ArrayList<>()).add(new String[]{table, column});
    }

    @Autowired
    private SysCodeSequenceMapper sysCodeSequenceMapper;

    /**
     * 按规则编码取一个业务单号（只读现有单号，取当天最小可用流水，不占号）。
     *
     * @param ruleCode sys_code_rule.rule_code，如 GD/RW/DHTZ
     * @return 形如 gd2026111400004 的单号
     */
    public String generateCode(String ruleCode)
    {
        return findCode(ruleCode, true);
    }

    public String previewCode(String ruleCode, String referenceCode)
    {
        return findChainCode(ruleCode, referenceCode, false);
    }

    private String findCode(String ruleCode, boolean reserve)
    {
        SysCodeRule rule = loadRule(ruleCode);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        String codePrefix = (rule.getPrefix() == null ? "" : rule.getPrefix())
                + buildDateSegment(rule.getDateFormat(), today);
        int serialPos = codePrefix.length() + 1;

        Set<Integer> used = new HashSet<>();
        for (String[] target : targets(ruleCode))
        {
            used.addAll(sysCodeSequenceMapper.selectUsedSerials(target[0], target[1], codePrefix, serialPos));
        }

        int serial = rule.getSerialStart() == null ? 1 : rule.getSerialStart().intValue();
        int maxSerial = 99999;
        for (; serial <= maxSerial; serial++) {
            if (used.contains(serial)) {
                continue;
            }
            String code = buildCode(rule, today, serial);
            if (available(code, reserve)) {
                return code;
            }
        }
        throw new IllegalStateException("当天单号已用尽：" + codePrefix);
    }

    /**
     * 按参照单号取「链号」：日期段与流水号完全沿用参照单号，只替换业务前缀。
     * 订单 dd2026112100004 -> 工单 gd2026112100004、批次 pc2026112100004、检验 zj2026112100004。
     *
     * <p>同一类单据在一张订单下出现多张时，首张用链号本身，之后依次追加 -01、-02 子序号，
     * 保证数字段始终一致且不会撞唯一索引。</p>
     *
     * @param ruleCode      目标单据规则编码
     * @param referenceCode 参照单号（订单号或工单号，带不带后缀均可）
     */
    public String generateChainCode(String ruleCode, String referenceCode)
    {
        return findChainCode(ruleCode, referenceCode, true);
    }

    private String findChainCode(String ruleCode, String referenceCode, boolean reserve)
    {
        ChainParts parts = parseChainParts(referenceCode);
        if (parts == null)
        {
            return findCode(ruleCode, reserve);
        }
        SysCodeRule rule = loadRule(ruleCode);
        String base = (rule.getPrefix() == null ? "" : rule.getPrefix())
                + parts.dateSegment + String.format("%05d", parts.serial.intValue());
        if (!isCodeUsed(ruleCode, base) && available(base, reserve))
        {
            return base;
        }
        for (int sub = 1; sub <= 99; sub++)
        {
            String candidate = base + "-" + String.format("%02d", sub);
            if (!isCodeUsed(ruleCode, candidate) && available(candidate, reserve))
            {
                return candidate;
            }
        }
        throw new IllegalStateException("链号子序号已用尽：" + base);
    }

    public String generateTaskCode(String workorderCode, int preferred)
    {
        if (workorderCode == null || workorderCode.trim().isEmpty() || workorderCode.length() > 60) {
            return generateCode("RW");
        }
        for (int sequence = Math.max(1, preferred); sequence <= 999; sequence++) {
            String code = workorderCode + "-" + String.format("%02d", sequence);
            if (!isCodeUsed("RW", code) && available(code, true)) {
                return code;
            }
        }
        throw new IllegalStateException("工单任务编号已用尽：" + workorderCode);
    }

    /** 从单号中提取日期段与流水号；带 -01 之类后缀时只取前 13 位数字。 */
    private ChainParts parseChainParts(String code)
    {
        if (code == null)
        {
            return null;
        }
        java.util.regex.Matcher matcher = CHAIN_PATTERN.matcher(code.trim());
        if (!matcher.find())
        {
            return null;
        }
        try
        {
            ChainParts parts = new ChainParts();
            parts.dateSegment = matcher.group(1);
            parts.serial = Integer.valueOf(matcher.group(2));
            return parts;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static final java.util.regex.Pattern CHAIN_PATTERN =
            java.util.regex.Pattern.compile("^[a-z]{2}([0-9]{8})([0-9]{5})");

    private static class ChainParts
    {
        private String dateSegment;
        private Integer serial;
    }

    /**
     * 按参照单号的流水号取下游单号，实现全链路单号对齐：订单 dd...0001 -> 工单 gd...0001。
     * 若该流水号在目标单据中已被占用则直接报错，避免静默顺延造成链路号码错位。
     */
    public String generateCodeWithSerial(String ruleCode, String referenceCode)
    {
        Integer serial = parseSerial(referenceCode);
        if (serial == null)
        {
            return generateCode(ruleCode);
        }
        SysCodeRule rule = loadRule(ruleCode);
        String code = buildCode(rule, LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")), serial.intValue());
        if (isCodeUsed(ruleCode, code))
        {
            throw new IllegalStateException("单号已被占用：" + code + "（须与订单流水号一致，请先处理冲突单据）");
        }
        return code;
    }

    private Integer parseSerial(String code)
    {
        if (code == null || code.length() < 5)
        {
            return null;
        }
        try
        {
            return Integer.valueOf(code.substring(code.length() - 5));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * 校验单号是否符合指定规则的当前格式（2位小写拼音 + 当天日期段 + 定长数字流水）。
     */
    public boolean isValidCode(String ruleCode, String code)
    {
        if (code == null)
        {
            return false;
        }
        SysCodeRule rule = sysCodeSequenceMapper.selectSysCodeRuleByCode(ruleCode);
        if (rule == null || (rule.getEnableFlag() != null && !"Y".equalsIgnoreCase(rule.getEnableFlag())))
        {
            return false;
        }
        String prefix = rule.getPrefix() == null ? "" : rule.getPrefix();
        String expectedPrefix = prefix + buildDateSegment(rule.getDateFormat(), LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));
        int length = rule.getSerialLength() == null ? 5 : rule.getSerialLength();
        if (code.length() != expectedPrefix.length() + length || !code.startsWith(expectedPrefix))
        {
            return false;
        }
        for (int i = expectedPrefix.length(); i < code.length(); i++)
        {
            if (!Character.isDigit(code.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 优先沿用调用方已预取且未被占用的合法单号；否则按最小可用号重新分配。
     * 预取号不落库、不占号，只有业务保存成功后才真正“消耗”该号。
     */
    public String ensureCode(String ruleCode, String code)
    {
        if (isValidCode(ruleCode, code) && !isCodeUsed(ruleCode, code) && available(code, true))
        {
            return code;
        }
        return generateCode(ruleCode);
    }

    /**
     * 把父工单号嵌入下游单号，便于从单号反查所属工单。
     * 目标字段长度为 64，拼接后超长时回退原单号，避免截断造成重号。
     */
    public String embedWorkorderCode(String workorderCode, String code)
    {
        if (workorderCode == null || workorderCode.trim().isEmpty() || code == null)
        {
            return code;
        }
        String merged = workorderCode.trim() + "-" + code;
        return merged.length() <= 64 ? merged : code;
    }

    /**
     * 单号是否已被业务表占用（软删除单据仍占用，避免历史单号复用）。
     */
    public boolean isCodeUsed(String ruleCode, String code)
    {
        for (String[] target : targets(ruleCode))
        {
            if (sysCodeSequenceMapper.countCode(target[0], target[1], code) > 0)
            {
                return true;
            }
        }
        return false;
    }

    private boolean available(String code, boolean reserve)
    {
        return reserve ? sysCodeSequenceMapper.reserveCode(code) == 1
                : sysCodeSequenceMapper.countReservedCode(code) == 0;
    }

    private SysCodeRule loadRule(String ruleCode)
    {
        SysCodeRule rule = sysCodeSequenceMapper.selectSysCodeRuleByCode(ruleCode);
        if (rule == null)
        {
            throw new IllegalStateException("编码规则未配置：" + ruleCode);
        }
        if (rule.getEnableFlag() != null && !"Y".equalsIgnoreCase(rule.getEnableFlag()))
        {
            throw new IllegalStateException("编码规则已停用：" + ruleCode);
        }
        // 单号硬约束：2位小写拼音首字母 + yyyyMMdd + 5位流水（00001-99999）
        if (rule.getPrefix() == null || !rule.getPrefix().matches("^[a-z]{2}$"))
        {
            throw new IllegalStateException("编码规则前缀必须是2位小写拼音首字母：" + ruleCode + "=" + rule.getPrefix());
        }
        if (!"yyyyMMdd".equals(rule.getDateFormat()))
        {
            throw new IllegalStateException("编码规则日期格式必须是yyyyMMdd：" + ruleCode);
        }
        if (rule.getSerialLength() == null || rule.getSerialLength() != 5)
        {
            throw new IllegalStateException("编码规则流水长度必须是5位：" + ruleCode);
        }
        targets(ruleCode);
        return rule;
    }

    private List<String[]> targets(String ruleCode)
    {
        List<String[]> list = CODE_TARGETS.get(ruleCode);
        if (list == null || list.isEmpty())
        {
            throw new IllegalStateException("编码规则未绑定业务表：" + ruleCode);
        }
        return list;
    }

    /**
     * 周期键：DAY=yyyyMMdd，MONTH=yyyyMM，YEAR=yyyy，NONE=空串。
     * 保留给其它按周期统计的场景使用。
     */
    static String buildPeriodKey(String resetCycle, LocalDate date)
    {
        String cycle = resetCycle == null ? "DAY" : resetCycle.trim().toUpperCase();
        switch (cycle) {
            case "YEAR":
                return String.format("%04d", date.getYear());
            case "MONTH":
                return String.format("%04d%02d", date.getYear(), date.getMonthValue());
            case "NONE":
                return "";
            case "DAY":
            default:
                return String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        }
    }

    /**
     * 拼装单号：前缀（可空）+ 日期段（可空）+ 定长流水。
     */
    static String buildCode(SysCodeRule rule, LocalDate date, long serial)
    {
        StringBuilder code = new StringBuilder();
        if (rule.getPrefix() != null) {
            code.append(rule.getPrefix());
        }
        String dateSegment = buildDateSegment(rule.getDateFormat(), date);
        if (dateSegment != null && !dateSegment.isEmpty()) {
            code.append(dateSegment);
        }
        int length = rule.getSerialLength() == null ? 5 : rule.getSerialLength();
        code.append(String.format("%0" + length + "d", serial));
        return code.toString();
    }

    /**
     * 日期段格式化（只支持 yyyy/MM/dd 组合，够用且无时区歧义）。
     */
    static String buildDateSegment(String pattern, LocalDate date)
    {
        if (pattern == null || pattern.trim().isEmpty()) {
            return "";
        }
        return pattern.trim()
                .replace("yyyy", String.format("%04d", date.getYear()))
                .replace("MM", String.format("%02d", date.getMonthValue()))
                .replace("dd", String.format("%02d", date.getDayOfMonth()));
    }
}
