package com.ktg.mes.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * MES 智能助手 —— 意图识别代理
 *
 * 设计要点：
 * 1. 大模型只做「自然语言 → 槽位 JSON」，不做任何数值计算；
 * 2. 双模型兜底：DeepSeek-chat（主）→ GLM-4-Flash（免费兜底，超时/报错时切换）；
 * 3. 都不行时返回 code=500，由前端回落到本地规则引擎并给出追问，绝不让演示卡住；
 * 4. API Key 全部从环境变量读取（MES_AI_KEY / MES_AI_FB_KEY），不落配置文件。
 */
@RestController
@RequestMapping("/mes/ai")
public class MesAiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(MesAiController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 密钥只从配置/环境变量读取（MES_AI_KEY / MES_AI_FB_KEY），代码中不保留明文

    @Value("${mes.ai.deepseek-key:}")
    private String deepseekKey;

    @Value("${mes.ai.deepseek-url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekUrl;

    @Value("${mes.ai.deepseek-model:deepseek-chat}")
    private String deepseekModel;

    @Value("${mes.ai.glm-key:}")
    private String glmKey;

    @Value("${mes.ai.glm-url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String glmUrl;

    @Value("${mes.ai.glm-model:glm-4-flash}")
    private String glmModel;

    @Value("${mes.ai.timeout-ms:3000}")
    private int timeoutMs;

    /** 本机兜底：配置项为空时直接读环境变量，避免环境差异导致密钥读不到 */
    @javax.annotation.PostConstruct
    private void applyEnvFallback() {
        if (!notBlank(deepseekKey)) {
            deepseekKey = envValue("MES_AI_KEY");
        }
        if (!notBlank(glmKey)) {
            glmKey = envValue("MES_AI_FB_KEY");
        }
    }

    private static String envValue(String name) {
        String v = System.getenv(name);
        return v == null ? "" : v.trim();
    }

    private static final String SYSTEM_PROMPT =
            "你是 MES 系统的意图解析器。把用户的中文问题解析成 JSON，只输出 JSON，不要解释。\n"
          + "字段：metric（stock 库存盘点 / inbound 入库 / outbound 出库 / workorder 工单 / inspection 检验 / alert 安全库存预警 / unknown）\n"
          + "timeText（原文中的时间词，如 10月份/本月/上月/近7天，没有则为空字符串）\n"
          + "category（物料名称或编码关键词，没有则为空字符串）\n"
          + "warehouse（仓库名关键词，没有则为空字符串）\n"
          + "confidence（0~1 的小数，表示你的把握）\n"
          + "如果问题与 MES 业务数据无关（如天气、闲聊、或系统中不存在的物料），metric 填 unknown。\n"
          + "示例输入：盘点一下10月份测温显示电控预装件的库存\n"
          + "示例输出：{\"metric\":\"stock\",\"timeText\":\"10月份\",\"category\":\"测温显示电控预装件\",\"warehouse\":\"\",\"confidence\":0.95}";

    /** 意图解析：主模型 → 兜底模型 → 失败 */
    @PostMapping("/intent")
    public AjaxResult intent(@RequestBody Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return AjaxResult.error("问题不能为空");
        }
        long start = System.currentTimeMillis();
        StringBuilder err = new StringBuilder();

        // 取 key：配置 → 系统属性(-D) → 环境变量，三重兜底，任一可用即可
        String dsKey = notBlank(deepseekKey) ? deepseekKey : readKey("MES_AI_KEY");
        String gKey = notBlank(glmKey) ? glmKey : readKey("MES_AI_FB_KEY");
        log.info("[AI] key 状态 deepseek={} glm={}", notBlank(dsKey), notBlank(gKey));

        // ① 主模型 DeepSeek
        if (notBlank(dsKey)) {
            try {
                Map<String, Object> slots = callModel(deepseekUrl, dsKey, deepseekModel, question);
                if (slots != null) {
                    slots.put("model", deepseekModel);
                    slots.put("costMs", System.currentTimeMillis() - start);
                    return AjaxResult.success(slots);
                }
            } catch (Exception e) {
                log.warn("[AI] DeepSeek 解析失败，切换兜底模型：{}", e.getMessage());
                err.append("deepseek: ").append(e.getMessage()).append("; ");
            }
        } else {
            err.append("deepseek: key 未配置; ");
        }

        // ② 兜底模型 GLM-4-Flash（免费）
        if (notBlank(gKey)) {
            try {
                Map<String, Object> slots = callModel(glmUrl, gKey, glmModel, question);
                if (slots != null) {
                    slots.put("model", glmModel);
                    slots.put("costMs", System.currentTimeMillis() - start);
                    return AjaxResult.success(slots);
                }
            } catch (Exception e) {
                log.warn("[AI] GLM 兜底失败：{}", e.getMessage());
                err.append("glm: ").append(e.getMessage()).append("; ");
            }
        } else {
            err.append("glm: key 未配置; ");
        }

        // ③ 都不可用：把真实原因带回去，便于前端/联调定位
        log.warn("[AI] 双模型均不可用：{}", err.toString());
        return AjaxResult.error("AI 暂时不可用，请稍后重试或联系管理员检查模型配置");
    }

    /** 配置自检：前端可用来判断是否启用大模型 */
    @PostMapping("/status")
    public AjaxResult status() {
        Map<String, Object> m = new HashMap<String, Object>();
        String dsKey = notBlank(deepseekKey) ? deepseekKey : readKey("MES_AI_KEY");
        String gKey = notBlank(glmKey) ? glmKey : readKey("MES_AI_FB_KEY");
        m.put("deepseek", notBlank(dsKey));
        m.put("glm", notBlank(gKey));
        m.put("model", notBlank(dsKey) ? deepseekModel : (notBlank(gKey) ? glmModel : "rules-only"));
        return AjaxResult.success(m);
    }

    /* ---------------- 内部实现 ---------------- */

    private Map<String, Object> callModel(String url, String apiKey, String model, String question) throws Exception {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("max_tokens", 200);
        payload.put("response_format", java.util.Collections.singletonMap("type", "json_object"));

        java.util.List<Map<String, String>> msgs = new java.util.ArrayList<Map<String, String>>();
        Map<String, String> sys = new HashMap<String, String>();
        sys.put("role", "system");
        sys.put("content", SYSTEM_PROMPT);
        msgs.add(sys);
        Map<String, String> usr = new HashMap<String, String>();
        usr.put("role", "user");
        usr.put("content", question);
        msgs.add(usr);
        payload.put("messages", msgs);

        String respBody = postJson(url, apiKey, MAPPER.writeValueAsString(payload));
        JsonNode root = MAPPER.readTree(respBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isEmpty()) {
            throw new IllegalStateException("模型返回为空");
        }
        String json = content.asText().trim();
        // 容错：剥离 ```json ``` 包裹
        if (json.startsWith("```")) {
            int i = json.indexOf('{');
            int j = json.lastIndexOf('}');
            if (i >= 0 && j > i) {
                json = json.substring(i, j + 1);
            }
        }
        JsonNode slotsNode = MAPPER.readTree(json);
        Map<String, Object> slots = new HashMap<String, Object>();
        slots.put("metric", textOf(slotsNode, "metric", "unknown"));
        slots.put("timeText", textOf(slotsNode, "timeText", ""));
        slots.put("category", textOf(slotsNode, "category", ""));
        slots.put("warehouse", textOf(slotsNode, "warehouse", ""));
        slots.put("confidence", slotsNode.path("confidence").asDouble(0.0));
        return slots;
    }

    private String postJson(String url, String apiKey, String json) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs * 2);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(data);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
            }
            if (code >= 400) {
                throw new IllegalStateException("HTTP " + code + ": " + sb.toString());
            }
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String textOf(JsonNode node, String field, String def) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? def : v.asText(def);
    }

    /** 取 key：系统属性(-D) → 环境变量 → 本地文件 E:/JavaCode/ai-keys.txt（联调兜底） */
    private static String readKey(String name) {
        String v = System.getProperty(name);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        v = System.getenv(name);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        try {
            java.io.File f = new java.io.File("E:/JavaCode/ai-keys.txt");
            if (f.exists()) {
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    int idx = line.indexOf('=');
                    if (idx > 0 && line.substring(0, idx).trim().equals(name)) {
                        String val = line.substring(idx + 1).trim();
                        br.close();
                        return val;
                    }
                }
                br.close();
            }
        } catch (Exception ignore) {
            // 忽略：由上层走规则兜底
        }
        return "";
    }

    private static boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }
}
