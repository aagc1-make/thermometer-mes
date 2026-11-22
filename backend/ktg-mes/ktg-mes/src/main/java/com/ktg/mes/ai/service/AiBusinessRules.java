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
    private static final Pattern ORDER_CODE = Pattern.compile("(?<![A-Z0-9-])(?:MO|WO)[A-Z0-9-]*\\d[A-Z0-9-]*(?![A-Z0-9-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH = Pattern.compile("(?:(20\\d{2})\\s*年\\s*)?(1[0-2]|0?[1-9])\\s*月(?:份)?");
    private static final Pattern DATE = Pattern.compile("20\\d{2}-\\d{2}-\\d{2}");

    private AiBusinessRules() {}

    static boolean preserveExistingAction(String question) {
        return question.matches("(?s).*(?:建单|新增|登记|创建|添加|修改|删除|撤销|过账|确认执行|全流程贯通|一键贯通|完整贯通).*");
    }

    static Call detect(String question, ObjectMapper json) {
        ObjectNode args = json.createObjectNode();
        String code;
        String order = match(ORDER_CODE, question);
        if (question.matches("(?is).*(?:安全库存|库存预警|低于安全线|缺货).*")) {
            String item = match(MATERIAL_CODE, question);
            if (item != null) args.put("query", item);
            return new Call("alerts", args);
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
        } else if (question.matches("(?s).*(?:库存|现存量|盘点|结存|在库量).*")) {
            code = "stock";
        } else if (question.matches("(?s).*(?:物料信息|物料规格|物料编码|物料单位).*") || match(MATERIAL_CODE, question) != null) {
            code = "materials";
        } else {
            return null;
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if (!(code.equals("bom") || code.equals("materials"))) {
            readDates(question, args, today, code.equals("movements"));
        }
        String query = order != null && "workorders".equals(code) ? order : match(MATERIAL_CODE, question);
        if (query == null && order == null && doesUseMaterialText(code)) {
            query = materialText(question);
        }
        if (query != null) args.put("query", query);
        if ("bom".equals(code) && query == null) return null;
        return new Call(code, args);
    }

    private static boolean doesUseMaterialText(String code) {
        return "stock".equals(code) || "bom".equals(code) || "materials".equals(code)
            || "movements".equals(code) || "inspections".equals(code);
    }

    private static String materialText(String question) {
        String text = MONTH.matcher(DATE.matcher(question).replaceAll(" ")).replaceAll(" ")
            .replaceAll("^(?:请问|请帮我|帮我|查询|查一下|盘点一下|盘点|列出|给我看下|看看)\\s*", "")
            .replaceAll("本月|这个月|上个月|上月|今天|现在|当前|今年|去年|截至月底|月末", " ")
            .replaceAll("(?i)(?:的)?(?:库存流水|库存(?:量|情况|结存)?|在库量|现存量|期末结存|BOM|物料清单|组件用量|物料信息|物料规格|物料编码|物料单位|出入库(?:流水)?|流水|入库|出库|(?:来料|过程|出货)?检验(?:记录|报告|单)?|不合格率|合格率|抽检量)(?:了|是)?(?:有)?(?:多少|有哪些|是什么|怎么样|情况|明细|列表|记录|如何)?", " ")
            .replaceAll("(?i)IQC|IPQC|OQC|合格率是多少|有多少|是多少|还有多少|多少|汇总|展开|查一下|哪些|所有|物料的|[?？。！!、，:：]", " ")
            .trim().replaceAll("^的|的$", "").trim();
        if (text.matches("(?:共|全部|物料|到|至|到月底|至月底|看一下|为|情况|有|怎么样)?")) return null;
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
