package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiBusinessRules {
    private static final Pattern MATERIAL_CODE = Pattern.compile("(?<![A-Z0-9-])[A-Z]{2,}(?:-[A-Z0-9]+){1,5}(?![A-Z0-9-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_CODE = Pattern.compile("(?<![A-Z0-9-])(?:GD[0-9]{13}(?:-[0-9]{2})?|(?:MO|WO)[A-Z0-9-]*\\d[A-Z0-9-]*)(?![A-Z0-9-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern SALES_ORDER_CODE = Pattern.compile("(?<![A-Z0-9-])DD[0-9]{13}(?![A-Z0-9-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH = Pattern.compile("(?:(20\\d{2})\\s*年\\s*)?(1[0-2]|0?[1-9])\\s*月(?:份)?");
    private static final Pattern DATE = Pattern.compile("20\\d{2}-\\d{2}-\\d{2}");
    private static final Pattern DATE_SPAN = Pattern.compile(DATE.pattern() + "\\s*(?:到|至|~|～|—|-)\\s*" + DATE.pattern());
    private static final String QUERY_SEPARATOR = "[\\s，,、：:]";
    // 单独的“下”后面要有分隔符，避免“查下壳”吞掉物料名里的“下”。
    private static final Pattern QUERY_PREFIX = Pattern.compile(
        "^" + QUERY_SEPARATOR + "*(?:(?:请问|请|麻烦|帮我|帮忙|给我|查(?:一查|查|询|看)?(?:一下|下(?="
            + QUERY_SEPARATOR + "))?|盘点(?:一下)?|列出|看(?:一下|看|下(?=" + QUERY_SEPARATOR + "))?)"
            + QUERY_SEPARATOR + "*)+");
    private static final Pattern NO_MATERIAL = Pattern.compile(
        "(?:(?:全部|还?有|有没有|有无|是否(?:还)?有)(?:\\s*物料)?|物料|共|到|至|到月底|至月底|看一下|为|情况|怎么样)?");

    /** “我们生产什么 / 成品是什么 / 有哪些产品”：答案是产品档案，既不是工单进度，也不是物料片段检索。 */
    private static final Pattern PRODUCT_CATALOG = Pattern.compile(
        "(?s).*(?:成品|产品目录|产品清单|产品型号|产品种类|主营|主打"
            + "|(?:生产|制造)(?:了)?(?:什么|哪些|哪几(?:个|种|款)?|多少种)"
            + "|生产(?:的)?(?:产品|成品)"
            + "|(?:做|造)(?:了)?(?:什么|哪些|哪几(?:个|种|款)?|多少种)?产品"
            + "|(?:公司|本厂|工厂|厂里|贵司|企业)(?:都|主要|目前)?(?:生产|制造|做)?的?产品).*");
    /** 带这些限定时问的是具体业务口径，交给对应工具（工序/库存/BOM/单据），不按产品目录回答。 */
    private static final Pattern PRODUCT_SCOPE = Pattern.compile(
        "(?s).*(?:工序|工艺|路线|设备|用料|物料|BOM|物料清单|组件用量|库存|结存|在库|入库|出库|流水"
            + "|检验|合格率|安全库存|预警|工单|进度|排期|交期|采购|销售|订单|客户|供应商"
            + "|用在|用于|装到|装在|装入|安装在|配套).*");

    private AiBusinessRules() {}

    static boolean preserveExistingAction(String question) {
        return question.matches("(?s).*(?:建单|新增|登记|创建|添加|修改|删除|撤销|过账|确认执行|全流程贯通|一键贯通|完整贯通).*");
    }

    /** 问本厂生产哪些产品：直接查产品档案，避免把“生产什么”理解成工单进度。 */
    static boolean productCatalog(String question) {
        return PRODUCT_CATALOG.matcher(question).matches() && !PRODUCT_SCOPE.matcher(question).matches();
    }

    static Call detect(String question, ObjectMapper json) {
        ObjectNode args = json.createObjectNode();
        String code;
        String order = match(ORDER_CODE, question);
        String salesOrder = match(SALES_ORDER_CODE, question);
        if (question.matches("(?is).*(?:安全库存|库存预警|低于安全线|缺货).*")) {
            code = "alerts";
        } else if ((order != null || salesOrder != null)
                && !question.matches("(?is).*(?:过程检验|IPQC|检验记录|检验报告|合格率|抽检量).*")) {
            code = "workorders";
            if (order != null) args.put("workorderCode", order);
            if (salesOrder != null) args.put("query", salesOrder);
        } else if (question.matches("(?is).*(?:BOM|物料清单|组件用量).*")) {
            code = "bom";
        } else if (question.matches("(?is).*(?:检验记录|检验报告|不合格率|合格率|抽检量|检验单|IQC|IPQC|OQC).*")) {
            code = "inspections";
            String kind = order != null || question.matches("(?is).*(?:过程|IPQC).*") ? "IPQC"
                : question.matches("(?is).*(?:出货|OQC).*") ? "OQC" : "IQC";
            args.put("kind", kind);
            if (order != null) args.put("workorderCode", order);
        } else if (question.matches("(?s).*(?:流水|入库多少|出库多少|入库了多少|出库了多少|出入库).*")) {
            if (question.matches("(?s).*(?:采购入库|领料出库|销售出库|成品入库).*")) return null;
            code = "movements";
            args.put("direction", question.contains("出入库") || (question.contains("出库") && question.contains("入库"))
                ? "ALL" : question.contains("入库") ? "IN" : question.contains("出库") ? "OUT" : "ALL");
        } else if (order != null || question.matches("(?s).*(?:工单|订单进度|生产进度).*")) {
            code = "workorders";
            if (question.matches("(?s).*(?:逾期|超期|滞后|落后|延期).*")) args.put("overdue", true);
        } else if (productCatalog(question)) {
            code = "products";
        } else if (question.matches("(?s).*(?:库存|现存量|盘点|结存|在库量).*")) {
            code = "stock";
        } else if (question.matches("(?s).*(?:物料信息|物料规格|物料编码|物料单位).*") || match(MATERIAL_CODE, question) != null) {
            code = "materials";
        } else {
            return null;
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if (!(code.equals("bom") || code.equals("materials") || code.equals("products") || code.equals("alerts"))) {
            readDates(question, args, today, code.equals("movements"));
        }
        String query = "workorders".equals(code) && args.has("workorderCode")
            ? null : match(MATERIAL_CODE, question);
        if (query == null && order == null && doesUseMaterialText(code)) {
            query = materialText(question);
        }
        if (query != null && !args.has("query")) args.put("query", query);
        if ("bom".equals(code) && query == null) return null;
        return new Call(code, args);
    }

    private static boolean doesUseMaterialText(String code) {
        return "stock".equals(code) || "bom".equals(code) || "materials".equals(code)
            || "movements".equals(code) || "inspections".equals(code) || "alerts".equals(code);
    }

    private static String materialText(String question) {
        String withoutDates = MONTH.matcher(DATE.matcher(DATE_SPAN.matcher(question).replaceAll(" ")).replaceAll(" "))
            .replaceAll(" ").replaceAll("本月|这个月|上个月|上月|今天|现在|当前|目前|今年|去年|截至月底|月末", " ");
        String text = QUERY_PREFIX.matcher(withoutDates).replaceFirst("")
            .replaceAll("(?:的)?(?:是否有?|有没有)?(?:低于(?:安全库存(?:阈值)?|安全线)|安全库存(?:预警)?|库存预警|缺货)(?:情况|明细|列表)?", " ")
            .replaceAll("(?i)(?:的)?(?:是否有?|有没有)?(?:库存流水|库存(?:量|情况|结存)?|在库量|现存量|期末结存|BOM|物料清单|组件用量|物料信息|物料规格|物料编码|物料单位|出入库(?:流水)?|流水|入库|出库|(?:来料|过程|出货)?检验(?:记录|报告|单)?|不合格率|合格率|抽检量)(?:了|是)?(?:有)?(?:多少|有哪些|是什么|怎么样|情况|明细|列表|记录|如何)?", " ")
            .replaceAll("(?i)IQC|IPQC|OQC|合格率是多少|有多少|是多少|还有多少|多少|汇总|展开|查一下|(?:还有|有)?哪些|所有|物料的|[?？。！!、，:：]", " ")
            .replaceAll("^\\s*(?:(?:里|中|内)?的)+\\s*", "")
            .trim().replaceFirst("(?:吗|呢|么)$", "").trim().replaceAll("^的|的$", "").trim();
        if (NO_MATERIAL.matcher(text).matches()) return null;
        return text.length() >= 2 && text.length() <= 80 ? text : null;
    }

    private static void readDates(String question, ObjectNode args, LocalDate today, boolean defaultMonth) {
        Matcher dates = DATE.matcher(question);
        if (dates.find()) {
            String first = dates.group();
            args.put("from", first);
            args.put("to", dates.find() ? dates.group() : first);
            return;
        }
        Matcher month = MONTH.matcher(question);
        YearMonth period = YearMonth.from(today);
        if (month.find()) {
            int monthValue = Integer.parseInt(month.group(2));
            int year = month.group(1) == null ? (question.contains("去年") ? today.getYear() - 1
                : question.contains("今年") || monthValue <= today.getMonthValue() ? today.getYear() : today.getYear() - 1)
                : Integer.parseInt(month.group(1));
            period = YearMonth.of(year, monthValue);
        } else if (question.contains("上月") || question.contains("上个月")) {
            period = period.minusMonths(1);
        } else if (question.contains("今天")) {
            args.put("from", today.toString());
            args.put("to", today.toString());
            return;
        } else if (!question.contains("本月") && !question.contains("这个月") && !defaultMonth) {
            return;
        }
        args.put("from", period.atDay(1).toString());
        args.put("to", period.atEndOfMonth().toString());
    }

    private static String match(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : null;
    }

    static class Call {
        final String code;
        final ObjectNode args;

        Call(String code, ObjectNode args) {
            this.code = code;
            this.args = args;
        }
    }
}
