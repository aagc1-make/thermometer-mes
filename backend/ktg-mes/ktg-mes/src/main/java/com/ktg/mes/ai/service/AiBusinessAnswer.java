package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ktg.mes.ai.service.KbRagEngines.object;

@Service
public class AiBusinessAnswer {
    private static final int MAX_CALLS = 3;
    private static final int MAX_CONTEXT_CHARS = 8000;
    private static final long MODEL_BUDGET_MS = 59000;
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final String SYSTEM = "你是MES数据问答的只读工具规划器。根据问题选择已注册的工具。"
        + "未来步骤可使用上一条工具结果中的合法工单编号。不要猜测物料名称、数量、结果或权限。"
        + "历史库存使用stock及完整起止日期；现存量不传日期。"
        + "物料筛选只填写名称或编码，不写问题整句。发现权限或参数错误后，只有参数能正确修正时才重试。"
        + "用户说的物料名可能口语化、用简称或拼写不一致：先调用 materials 按关键词检索，选最符合的准确名称或编码，"
        + "再用它去查库存、工单或 BOM；不要直接把用户原话当作物料名。"
        + "工具返回 noExactMatch 时，可从 similarItems 里选最接近的准确名称重试一次，不要臆造名称。"
        + "需要业务确认、超出工具能力或已经取得足够结果时，不再调用工具。";
    private static final String NARRATE_SYSTEM = "你是温衡智造 MES 智能助手。用一到三句自然、口语化的中文回答用户问题。"
        + "如果给的是查询结果，只能使用其中的事实（条数、字段值、日期、编码）；结果为空时直接说没有符合条件的记录。"
        + "如果给的是已经写好的系统说明，请在不改变事实的前提下改写成更自然的说法，保留全部数字、编号和权限说明，不新增信息。"
        + "原文中的 [1]、[2] 等引用编号必须原样保留。"
        + "不要重新计算，不要推断缺失值，不要编造数据。只输出正文，不要标题、不要 Markdown、不要列表符号。";
    private final AiReadTools tools;
    private final KbRagEngines engines;
    private final ObjectMapper json;

    public AiBusinessAnswer(AiReadTools tools, KbRagEngines engines, ObjectMapper json) {
        this.tools = tools;
        this.engines = engines;
        this.json = json;
    }

    public Map<String, Object> answer(String question, boolean allowExternal, boolean skipLocal) {
        if (AiBusinessRules.preserveExistingAction(question)) {
            return legacy();
        }
        long started = System.currentTimeMillis();
        List<AiReadTools.Result> results = new ArrayList<>();
        List<Map<String, Object>> trace = new ArrayList<>();
        int[] calls = {0};
        for (KbRagEngines.ToolChannel channel : engines.toolChannels(allowExternal, skipLocal)) {
            if (calls[0] >= MAX_CALLS || System.currentTimeMillis() - started >= MODEL_BUDGET_MS) break;
            String mode = channel.isExternal() ? "external" : "local";
            addTrace(trace, channel.isExternal() ? "外部 API 规划" : "本地模型规划", channel.model, started);
            String issue = runChannel(channel, question, started, calls, results, trace);
            if (!results.isEmpty()) {
                return response(mode, channel.model, results, trace, issue);
            }
            if (issue == null) {
                // 模型正常应答但判断不需要查数据：不再换渠道重试，直接走规则/自然回答
                addTrace(trace, "无需查询", "模型判断本题不需要业务数据", started);
                break;
            }
            addTrace(trace, "回退查询", issue, started);
        }
        AiBusinessRules.Call fallback = AiBusinessRules.detect(question, json);
        if (fallback == null) {
            return legacy();
        }
        if (calls[0] >= MAX_CALLS) {
            throw new ServiceException("本轮没有取得可用的数据，请缩小查询范围或提供完整物料编码。", HttpStatus.BAD_REQUEST);
        }
        return answerFromRule(fallback, started, trace, "规则引擎兜底",
            skipLocal ? "本轮已模拟本地模型离线" : "使用固定的业务查询规则");
    }

    /**
     * 把工具结果整理成自然语言。优先交给已授权的大模型总结（外部优先、本地兜底），
     * 模型不可用得才使用确定性描述，保证表格与数字仍然可查。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> addAnswer(String question, Map<String, Object> response, boolean allowExternal, boolean skipLocal) {
        List<AiReadTools.Result> results = (List<AiReadTools.Result>) response.get("results");
        if (results == null || results.isEmpty()) return response;

        response.put("answer", summary(results));
        if (skipLocal) return response;
        if (!allowExternal && !engines.localAvailable()) return response;

        try {
            String verified = json.writeValueAsString(factsFor(results));
            if (verified.length() > 20000) return response;
            String summary = engines.naturalText(allowExternal, NARRATE_SYSTEM, narratePrompt(question, verified));
            if (acceptable(summary, verified)) {
                response.put("answer", summary);
            }
        } catch (IOException ignored) {
            // 保留确定性描述，表格数据仍可查看
        }
        return response;
    }

    /** 仅按事实做自然语言叙述，供前端把规则命中的表格/清单改写成自然回答。 */
    public String narrate(boolean allowExternal, String question, String factsJson) {
        String text = engines.naturalText(allowExternal, NARRATE_SYSTEM, narratePrompt(question, factsJson));
        return acceptable(text, factsJson) ? text : "";
    }

    private static List<Map<String, Object>> factsFor(List<AiReadTools.Result> results) {
        List<Map<String, Object>> facts = new ArrayList<>();
        for (AiReadTools.Result result : results) {
            Map<String, Object> fact = object("query", result.getLabel(), "filters", result.getArguments(),
                "matched", result.getTotal(), "columns", result.getColumns());
            if (result.getRows().size() == result.getTotal() && result.getTotal() <= 30) {
                fact.put("rows", result.getRows());
            }
            facts.add(fact);
        }
        return facts;
    }

    private static boolean acceptable(String summary, String verified) {
        return !summary.isEmpty() && summary.length() <= 600 && numbersIn(verified).containsAll(numbersIn(summary));
    }

    private static String narratePrompt(String question, String facts) {
        return "用户问题：" + question + "\n查询结果（JSON）：" + facts;
    }

    private static Set<String> numbersIn(String text) {
        Set<String> numbers = new HashSet<>();
        Matcher matcher = NUMBER.matcher(text.replaceAll("(?<=\\d)[,，](?=\\d{3}(?:\\D|$))", ""));
        while (matcher.find()) {
            numbers.add(new BigDecimal(matcher.group()).stripTrailingZeros().toPlainString());
        }
        return numbers;
    }

    /** 业务结果的确定性汇总：混合问答与退化路径共用，保证页面数字与只读工具查询一致。 */
    static String summary(List<AiReadTools.Result> results) {
        return results.stream().map(AiBusinessAnswer::describeResult).collect(Collectors.joining("；")) + "。";
    }

    private static String describeResult(AiReadTools.Result result) {
        Map<String, Object> args = result.getArguments();
        String scope = args.containsKey("query") ? "（" + args.get("query") + "）" : "";
        String period = args.containsKey("from") ? args.get("from") + " 至 " + args.get("to") + "，" : "";
        if ("stock".equals(result.getCode()) && result.getTotal() == 1 && result.getRows().size() == 1) {
            Map<String, Object> row = result.getRows().get(0);
            if (row.get("balance") != null && row.get("itemName") != null) {
                String unit = row.get("unit") == null ? "（单位未标注）" : String.valueOf(row.get("unit"));
                return period + row.get("itemName") + (period.isEmpty() ? "当前库存为 " : "期末库存为 ") + row.get("balance") + " " + unit;
            }
        }
        return period + result.getLabel() + scope + (result.getTotal() == 0
            ? "未找到匹配记录" : "共匹配 " + result.getTotal() + " 条记录");
    }

    AiBusinessRules.Call findRule(String question) {
        return AiBusinessRules.preserveExistingAction(question) ? null : AiBusinessRules.detect(question, json);
    }

    Map<String, Object> answerFromRule(AiBusinessRules.Call rule, long started, List<Map<String, Object>> trace,
                                      String label, String detail) {
        addTrace(trace, label, detail, started);
        AiReadTools.Result result = tools.execute(rule.code, rule.args);
        addToolTrace(trace, result, started);
        return response("rules", null, Collections.singletonList(result), trace, null);
    }

    private String runChannel(KbRagEngines.ToolChannel channel, String question, long started, int[] calls,
                              List<AiReadTools.Result> results, List<Map<String, Object>> trace) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(object("role", "system", "content", SYSTEM + " 当前日期为 "
            + LocalDate.now(ZoneId.of("Asia/Shanghai")) + "。"));
        messages.add(object("role", "user", "content", question));
        Set<String> signature = new HashSet<>();
        try {
            for (int round = 0; round < MAX_CALLS && calls[0] < MAX_CALLS; round++) {
                long remaining = MODEL_BUDGET_MS - (System.currentTimeMillis() - started);
                if (remaining <= 0 || json.writeValueAsString(messages).length() > MAX_CONTEXT_CHARS) {
                    return "已达到本轮规划限制";
                }
                JsonNode planned = channel.plan(messages, tools.modelSchemas(), (int) Math.min(29000, remaining))
                    .path("tool_calls");
                if (!planned.isArray() || planned.size() == 0) {
                    return null;
                }
                List<Map<String, Object>> sentCalls = new ArrayList<>();
                List<Map<String, Object>> toolReplies = new ArrayList<>();
                for (JsonNode action : planned) {
                    if (calls[0] >= MAX_CALLS) {
                        return "已达单轮工具上限，以下为已完成的查询。";
                    }
                    calls[0]++;
                    JsonNode function = action.path("function");
                    JsonNode args;
                    try {
                        JsonNode arguments = function.path("arguments");
                        args = arguments.isTextual() ? json.readTree(arguments.asText()) : arguments;
                    } catch (IOException e) {
                        addTrace(trace, "工具参数无效", "模型返回的参数无法读取", started);
                        continue;
                    }
                    String code = function.path("name").asText();
                    String argsJson = json.writeValueAsString(args);
                    if (argsJson.length() > 700 || !signature.add(code + ":" + argsJson)) {
                        return "已跳过重复查询或过长参数。";
                    }
                    String id = "call_" + calls[0];
                    sentCalls.add(object("id", id, "type", "function",
                        "function", object("name", code, "arguments", argsJson)));
                    String content;
                    try {
                        AiReadTools.Result result = tools.execute(code, args);
                        results.add(result);
                        addToolTrace(trace, result, started);
                        content = resultForModel(result, channel.isExternal());
                    } catch (ServiceException e) {
                        if (Integer.valueOf(HttpStatus.FORBIDDEN).equals(e.getCode())) throw e;
                        addTrace(trace, "工具参数被拒绝", e.getMessage(), started);
                        content = json.writeValueAsString(object("error", e.getMessage()));
                    } catch (RuntimeException e) {
                        addTrace(trace, "工具查询失败", "数据库未返回可用数据", started);
                        return "其中一项查询未完成。";
                    }
                    toolReplies.add(object("role", "tool", "tool_call_id", id, "content", content));
                }
                if (sentCalls.isEmpty()) return "模型未能提供合法参数";
                messages.add(object("role", "assistant", "content", "", "tool_calls", sentCalls));
                messages.addAll(toolReplies);
            }
        } catch (IOException e) {
            return "模型暂不可用或规划超时";
        }
        return null;
    }

    private String resultForModel(AiReadTools.Result result, boolean external) throws IOException {
        Map<String, Object> base = object("tool", result.getCode(), "matchedCount", result.getTotal());
        if (result.getTotal() == 0 && !result.getSuggestions().isEmpty()) {
            base.put("noExactMatch", true);
            base.put("similarItems", result.getSuggestions());
            base.put("hint", "没有精确匹配。如果这些相近物料与问题所指相同，请用它的准确名称再调用一次工具。");
        }
        if (!external) {
            base.put("rows", result.getRows().subList(0, Math.min(result.getRows().size(), 3)));
            if (json.writeValueAsString(base).length() > 1400) {
                base.remove("rows");
            }
        }
        return json.writeValueAsString(base);
    }

    private static void addTrace(List<Map<String, Object>> trace, String label, String detail, long started) {
        trace.add(object("label", label, "detail", detail, "elapsedMs", System.currentTimeMillis() - started));
    }

    private static void addToolTrace(List<Map<String, Object>> trace, AiReadTools.Result result, long started) {
        trace.add(object("label", result.getLabel(), "detail", "返回 " + result.getRows().size() + " 条 · " + result.getSource(),
            "elapsedMs", System.currentTimeMillis() - started, "queryMs", result.getElapsedMs(),
            "arguments", result.getArguments(), "rows", result.getRows().size(), "queriedAt", result.getQueriedAt()));
    }

    private Map<String, Object> response(String mode, String model, List<AiReadTools.Result> results,
                                                List<Map<String, Object>> trace, String issue) {
        return object("mode", mode, "model", model, "results", distinct(results), "trace", trace, "notice", issue);
    }

    /** 模型可能用相近名称重复查询（例如纠正后按名称和编码各查一次），同一份结果只保留一条。 */
    private List<AiReadTools.Result> distinct(List<AiReadTools.Result> results) {
        Map<String, AiReadTools.Result> unique = new LinkedHashMap<>();
        for (AiReadTools.Result result : results) {
            String key;
            try {
                key = result.getRows().isEmpty()
                    ? result.getCode() + "|empty|" + result.getArguments().getOrDefault("query", "")
                    : result.getCode() + "|rows|" + json.writeValueAsString(result.getRows());
            } catch (IOException e) {
                key = result.getCode() + "|" + result.getTotal() + "|" + result.getArguments();
            }
            unique.putIfAbsent(key, result);
        }
        return new ArrayList<>(unique.values());
    }

    private static Map<String, Object> legacy() {
        return object("mode", "legacy");
    }
}
