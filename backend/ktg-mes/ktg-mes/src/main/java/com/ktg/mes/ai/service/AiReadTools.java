package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.enums.BusinessStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.SecurityUtils;
import com.ktg.common.utils.ServletUtils;
import com.ktg.common.utils.ip.IpUtils;
import com.ktg.framework.manager.AsyncManager;
import com.ktg.framework.manager.factory.AsyncFactory;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.mapper.AiReadToolMapper;
import com.ktg.system.domain.SysOperLog;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ktg.mes.ai.service.KbRagEngines.object;

@Service
public class AiReadTools {
    private static final Logger log = LoggerFactory.getLogger(AiReadTools.class);
    private static final String STOCK = "mes:wm:wmstock:list";
    private static final String MOVEMENTS = "mes:wm:transaction:list";
    private static final String MATERIAL = "mes:md:mditem:list";

    private final PermissionService permissions;
    private final AiReadToolMapper mapper;
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public AiReadTools(PermissionService permissions, AiReadToolMapper mapper) {
        this.permissions = permissions;
        this.mapper = mapper;
        register("stock", "库存查询", "查当前库存或指定日期区间的期初、入库、出库和期末量，返回各物料库存。",
            STOCK, "wm_material_stock、md_item", mapper::stock, "query,from,to,limit", "",
            "itemCode:物料编码,itemName:物料名称,unit:单位,opening:期初,incoming:入库,outgoing:出库,balance:库存");
        register("movements", "出入库流水", "按物料汇总指定区间库存流水，方向可为入库、出库或全部。",
            MOVEMENTS, "wm_transaction", mapper::movements, "query,from,to,direction,limit", "from,to",
            "itemCode:物料编码,itemName:物料名称,unit:单位,incoming:入库,outgoing:出库,net:净变动,transactionCount:流水数");
        register("workorders", "工单查询与进度", "查生产工单交期、状态、计划量、已生产量及进度，支持筛逾期。",
            "mes:pro:workorder:list", "pro_workorder", mapper::workorders, "query,status,from,to,overdue,limit", "",
            "workorderCode:工单号,workorderName:工单名称,productName:产品,planned:计划量,produced:已生产,progress:进度（%）,status:状态,dueDate:交期");
        register("bom", "BOM 展开", "按成品或半成品编码或名称，最多展开六级 BOM，并汇总单件产品的组件用量。",
            MATERIAL, "md_product_bom、md_item", this::readBom, "query,limit", "query",
            "itemCode:组件编码,itemName:组件名称,depth:层级,unit:单位,quantity:单件累计用量");
        register("inspections", "检验记录查询", "按检验类型及物料汇总已完成检验的记录数、样本、合格数量和合格率。过程检验可按工单编号过滤。",
            "mes:qc:iqc:list", "qc_iqc", mapper::inspections, "query,kind,workorderCode,from,to,limit", "kind",
            "itemCode:物料编码,itemName:物料名称,records:检验单数,inspected:抽检量,qualified:合格量,unqualified:不合格量,passRate:合格率（%）,failRate:不合格率（%）");
        register("alerts", "安全库存预警", "列出低于安全库存阈值或缺货的物料和缺口。",
            STOCK, "v_stock_alert", mapper::alerts, "query,limit", "",
            "itemCode:物料编码,itemName:物料名称,unit:单位,balance:现存量,minStock:安全库存,shortage:缺口,alert:等级");
        register("materials", "物料信息", "查询物料编码、名称、类别、规格和单位。",
            MATERIAL, "md_item", mapper::materials, "query,limit", "",
            "itemCode:物料编码,itemName:物料名称,specification:规格,unit:单位,category:类别,status:状态");
    }

    public List<Map<String, Object>> manifest() {
        return tools.values().stream().map(tool -> object(
            "code", tool.code, "label", tool.label, "description", tool.description,
            "permission", "inspections".equals(tool.code)
                ? "mes:qc:iqc:list / mes:qc:ipqc:list / mes:qc:oqc:list" : tool.permission,
            "readOnly", true, "schema", tool.schema()
        )).collect(Collectors.toList());
    }

    List<Map<String, Object>> modelSchemas() {
        return tools.values().stream().map(tool -> object("type", "function", "function",
            object("name", tool.code, "description", tool.description, "parameters", tool.schema())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true, timeout = 6)
    public Map<String, Object> purchaseReceipts(String itemCode, String from, String to, Integer requestedPage) {
        LocalDate start = receiptDate(from);
        LocalDate end = receiptDate(to);
        if (end.isBefore(start) || (requestedPage != null && requestedPage < 1)) {
            throw invalid("请提供有效的查询时间和页码。");
        }
        if (itemCode != null && (itemCode.trim().isEmpty() || itemCode.length() > 80)) {
            throw invalid("请提供有效的物料编码。");
        }
        Map<String, Object> scope = object("itemCode", itemCode, "from", from, "to", to);
        int total = mapper.countPurchaseReceipts(scope);
        if (requestedPage == null || total == 0) {
            return object("total", total, "results", Collections.emptyList());
        }

        long started = System.currentTimeMillis();
        int pageSize = 25;
        int lastPage = (total - 1) / pageSize + 1;
        int page = Math.min(requestedPage, lastPage);
        int offset = (page - 1) * pageSize;
        boolean includeVendor = permissions.hasPermi("mes:wm:itemrecpt:list") || permissions.hasPermi("mes:wm:itemrecpt:query");
        Map<String, Object> params = new LinkedHashMap<>(scope);
        params.putAll(object("includeVendor", includeVendor, "limit", pageSize, "offset", offset));
        List<Map<String, String>> columns = new ArrayList<>();
        receiptColumn(columns, "receivedAt", "入库时间");
        receiptColumn(columns, "receiptCode", "入库单号");
        receiptColumn(columns, "itemName", "物料");
        receiptColumn(columns, "quantity", "数量");
        receiptColumn(columns, "unit", "单位");
        receiptColumn(columns, "warehouseName", "仓库");
        if (includeVendor) receiptColumn(columns, "vendorName", "供应商");
        Result result = new Result("purchaseReceipts", "采购入库流水明细", "库存流水", scope, columns,
            mapper.purchaseReceiptRows(params), total, System.currentTimeMillis() - started, Instant.now().toString());
        return object("total", total, "results", Collections.singletonList(result),
            "page", page, "lastPage", lastPage, "offset", offset);
    }

    private static LocalDate receiptDate(String value) {
        try {
            if (value != null && value.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Report the same input error for malformed and nonexistent dates.
        }
        throw invalid("日期应为有效的 YYYY-MM-DD。");
    }

    private static void receiptColumn(List<Map<String, String>> columns, String prop, String label) {
        Map<String, String> column = new LinkedHashMap<>();
        column.put("prop", prop);
        column.put("label", label);
        columns.add(column);
    }

    @Transactional(readOnly = true, timeout = 4)
    public Result execute(String code, JsonNode input) {
        Tool tool = tools.get(code);
        if (tool == null) {
            throw invalid("模型请求了未注册的工具。");
        }
        long started = System.currentTimeMillis();
        Map<String, Object> args = Collections.emptyMap();
        try {
            args = validate(tool, input);
            String permission = tool.permission;
            String source = tool.source;
            if ("inspections".equals(code)) {
                String stage = ((String) args.get("kind")).toLowerCase(java.util.Locale.ROOT);
                permission = "mes:qc:" + stage + ":list";
                source = "qc_" + stage;
            }
            checkPermission(permission, tool.label);
            if ("stock".equals(code) && args.containsKey("from")) {
                checkPermission(MOVEMENTS, "指定期间库存");
                source += "、wm_transaction";
            }
            List<Map<String, Object>> rows = tool.reader.apply(args);
            int total = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("matchedCount")).intValue();
            rows.forEach(row -> row.remove("matchedCount"));
            List<Map<String, String>> displayed = tool.columns.stream()
                .filter(column -> !("stock".equals(code) && !input.has("from")
                    && Arrays.asList("opening", "incoming", "outgoing").contains(column.get("prop"))))
                .collect(Collectors.toList());
            long duration = System.currentTimeMillis() - started;
            audit(tool, args, rows.size(), duration, null);
            return new Result(tool.code, tool.label, source, args, displayed, rows, total, duration, Instant.now().toString());
        } catch (RuntimeException e) {
            audit(tool, args, 0, System.currentTimeMillis() - started,
                e instanceof ServiceException ? e.getMessage() : "查询未完成");
            throw e;
        }
    }

    private static Map<String, Object> validate(Tool tool, JsonNode input) {
        if (input == null || !input.isObject()) {
            throw invalid("工具参数必须为 JSON 对象。");
        }
        Map<String, Object> args = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = input.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            Parameter parameter = tool.parameters.get(field.getKey());
            if (parameter == null) {
                throw invalid("工具包含不支持的参数。");
            }
            args.put(field.getKey(), parameter.read(field.getValue()));
        }
        if (!args.keySet().containsAll(tool.required)) {
            throw invalid("缺少工具查询所需的参数。");
        }
        if (args.containsKey("from") || args.containsKey("to")) {
            if (!args.containsKey("from") || !args.containsKey("to")) {
                throw invalid("请选择完整的日期区间。");
            }
            LocalDate from = LocalDate.parse((String) args.get("from"));
            LocalDate to = LocalDate.parse((String) args.get("to"));
            long span = ChronoUnit.DAYS.between(from, to);
            if (span < 0 || span >= 366) {
                throw invalid("查询日期区间最长为 366 天。");
            }
        }
        if (args.containsKey("workorderCode") && !"IPQC".equals(args.get("kind"))) {
            throw invalid("按工单查询时请选择过程检验。");
        }
        args.putIfAbsent("limit", 12);
        return args;
    }

    private void checkPermission(String permission, String label) {
        if (!permissions.hasPermi(permission)) {
            throw new ServiceException("当前账号没有「" + label + "」的查询权限。", HttpStatus.FORBIDDEN);
        }
    }

    private List<Map<String, Object>> readBom(Map<String, Object> args) {
        List<Map<String, Object>> matched = mapper.materialCandidates(args);
        if (matched.isEmpty()) {
            return Collections.emptyList();
        }
        if (matched.size() > 1) {
            boolean firstExact = Boolean.TRUE.equals(matched.get(0).get("exactMatch"))
                || "1".equals(String.valueOf(matched.get(0).get("exactMatch")));
            Object second = matched.get(1).get("exactMatch");
            if (!firstExact || Boolean.TRUE.equals(second) || "1".equals(String.valueOf(second))) {
                throw invalid("匹配到多个产品，请使用完整的物料编码查询 BOM。");
            }
        }
        Map<String, Object> query = new LinkedHashMap<>(args);
        query.put("itemId", matched.get(0).get("itemId"));
        return mapper.bom(query);
    }

    private void register(String code, String label, String description, String permission, String source,
                          Function<Map<String, Object>, List<Map<String, Object>>> reader,
                          String accepted, String required, String columns) {
        Tool tool = new Tool(code, label, description, permission, source, reader);
        for (String field : accepted.split(",")) {
            tool.parameters.put(field, Parameter.valueOf(field));
        }
        if (!required.isEmpty()) {
            tool.required.addAll(Arrays.asList(required.split(",")));
        }
        for (String definition : columns.split(",")) {
            String[] pair = definition.split(":", 2);
            Map<String, String> column = new LinkedHashMap<>();
            column.put("prop", pair[0]);
            column.put("label", pair[1]);
            tool.columns.add(column);
        }
        tools.put(code, tool);
    }

    private static ServiceException invalid(String message) {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }

    private static void audit(Tool tool, Map<String, Object> args, int count, long duration, String failure) {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            SysOperLog entry = new SysOperLog();
            entry.setTitle("智能助手只读工具");
            entry.setMethod(tool.code);
            entry.setOperName(SecurityUtils.getUsername());
            entry.setOperUrl(request.getRequestURI());
            entry.setRequestMethod(request.getMethod());
            entry.setOperIp(IpUtils.getIpAddr(request));
            Map<String, Object> masked = new LinkedHashMap<>(args);
            masked.computeIfPresent("query", (key, value) -> "[已遮盖]");
            masked.computeIfPresent("workorderCode", (key, value) -> "[已遮盖]");
            entry.setOperParam(masked.toString());
            entry.setJsonResult("rows=" + count + ", elapsedMs=" + duration);
            entry.setStatus(failure == null ? BusinessStatus.SUCCESS.ordinal() : BusinessStatus.FAIL.ordinal());
            entry.setErrorMsg(failure);
            AsyncManager.me().execute(AsyncFactory.recordOper(entry));
        } catch (RuntimeException e) {
            log.warn("未能记录只读工具审计，工具 {}", tool.code, e);
        }
    }

    private static class Tool {
        final String code;
        final String label;
        final String description;
        final String permission;
        final String source;
        final Function<Map<String, Object>, List<Map<String, Object>>> reader;
        final Map<String, Parameter> parameters = new LinkedHashMap<>();
        final List<String> required = new ArrayList<>();
        final List<Map<String, String>> columns = new ArrayList<>();

        Tool(String code, String label, String description, String permission, String source,
             Function<Map<String, Object>, List<Map<String, Object>>> reader) {
            this.code = code;
            this.label = label;
            this.description = description;
            this.permission = permission;
            this.source = source;
            this.reader = reader;
        }

        Map<String, Object> schema() {
            Map<String, Object> properties = new LinkedHashMap<>();
            parameters.forEach((name, definition) -> properties.put(name, definition.schema));
            return object("type", "object", "properties", properties, "required", required, "additionalProperties", false);
        }
    }

    private enum Parameter {
        query(object("type", "string", "minLength", 1, "maxLength", 80, "description", "只传物料、产品或工单的名称或编码")),
        from(object("type", "string", "format", "date", "description", "YYYY-MM-DD，含起日")),
        to(object("type", "string", "format", "date", "description", "YYYY-MM-DD，含止日")),
        direction(object("type", "string", "enum", Arrays.asList("IN", "OUT", "ALL"))),
        kind(object("type", "string", "enum", Arrays.asList("IQC", "IPQC", "OQC"))),
        status(object("type", "string", "enum", Arrays.asList("PREPARE", "CONFIRMED", "FINISHED", "CANCELED"))),
        workorderCode(object("type", "string", "minLength", 1, "maxLength", 64, "description", "过程检验关联的确切工单编号")),
        overdue(object("type", "boolean")),
        limit(object("type", "integer", "minimum", 1, "maximum", 200));

        final Map<String, Object> schema;

        Parameter(Map<String, Object> schema) {
            this.schema = schema;
        }

        Object read(JsonNode value) {
            if (this == limit) {
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1 || value.intValue() > 200) {
                    throw invalid("返回行数必须在 1 到 200 之间。");
                }
                return value.intValue();
            }
            if (this == overdue) {
                if (!value.isBoolean()) throw invalid("逾期筛选参数必须为布尔值。");
                return value.booleanValue();
            }
            if (!value.isTextual()) throw invalid("工具要求文本参数。");
            String text = value.asText().trim();
            if (text.isEmpty() || text.length() > (this == workorderCode ? 64 : 80)
                || text.codePoints().anyMatch(Character::isISOControl)) {
                throw invalid("查询文本参数无效。");
            }
            if (this == from || this == to) {
                try {
                    if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("Invalid date", text, 0);
                    LocalDate.parse(text);
                } catch (DateTimeParseException e) {
                    throw invalid("日期应为有效的 YYYY-MM-DD。");
                }
            }
            Object choices = schema.get("enum");
            if (choices instanceof List && !((List<?>) choices).contains(text)) {
                throw invalid("工具枚举参数无效。");
            }
            return text;
        }
    }

    @Value
    public static class Result {
        String code;
        String label;
        String source;
        Map<String, Object> arguments;
        List<Map<String, String>> columns;
        List<Map<String, Object>> rows;
        int total;
        long elapsedMs;
        String queriedAt;
    }
}
