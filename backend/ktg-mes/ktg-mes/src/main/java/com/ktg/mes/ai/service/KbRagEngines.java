package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Component
public class KbRagEngines {
    /** 检索或工具都拿不到内容时的助手人设：允许自然对话，也说明能力边界。 */
    public static final String ASSISTANT_SYSTEM = "你是温衡智造 MES 智能助手，服务对象是车间、计划、仓库与质检人员。"
        + "用简洁自然的中文回答，可以问候、闲聊、解释 MES 概念并给出使用建议。"
        + "业务数据只能来自只读工具或授权文档；本轮没有取得这些内容时，直接说明暂未查到，并给出更具体的提问建议（如物料编码、工单号、检验类型）。"
        + "不得编造库存、工单、检验等业务数据，不得声称已修改系统。不要输出思考过程。"
        + "系统模块：主数据（物料、客户、供应商、车间、工作站、多级 BOM）；"
        + "仓储（到货通知、采购入库、生产领料退料、产品入库、销售出库、库存流水）；"
        + "生产（工单、排产、报工、工序流转、工艺路线、安灯）；"
        + "质量（来料/过程/出货/退料检验、不良记录、批次追溯）；"
        + "设备（台账、点检保养、维修、工装夹具）；班组排班（班组、排班计划、班次、节假日）；"
        + "系统管理（用户、角色、菜单、部门、岗位、字典、参数、日志、定时任务）。"
        + "能直接执行的写操作只有：新增到货通知、创建生产工单草稿；其余写操作需到对应菜单完成。"
        + "回答用纯文本自然段落，不要使用 Markdown 标记（不要 **、#、- 列表、代码块），可以用中文分句和换行。";

    private final ObjectMapper json;

    @Value("${mes.ai.rag.ollama-url:http://127.0.0.1:11434}")
    private String ollama;
    @Value("${mes.ai.rag.local-model:qwen2.5:7b-instruct-q4_K_M}")
    private String localModel;
    @Value("${mes.ai.rag.embedding-model:bge-m3}")
    private String embeddingModel;
    @Value("${mes.ai.rag.qdrant-url:http://127.0.0.1:6333}")
    private String qdrant;
    @Value("${mes.ai.rag.qdrant-key:}")
    private String qdrantKey;
    @Value("${mes.ai.rag.collection:mes_kb_bge_m3_v1}")
    private String collection;
    @Value("${mes.ai.rag.allow-external:true}")
    private boolean externalEnabled;

    @Value("${mes.ai.deepseek-url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekUrl;
    @Value("${mes.ai.deepseek-key:}")
    private String deepseekKey;
    @Value("${mes.ai.deepseek-model:deepseek-chat}")
    private String deepseekModel;
    @Value("${mes.ai.glm-url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String glmUrl;
    @Value("${mes.ai.glm-key:}")
    private String glmKey;
    @Value("${mes.ai.glm-model:glm-4-flash}")
    private String glmModel;

    public KbRagEngines(ObjectMapper json) {
        this.json = json;
    }

    public Map<String, Object> status() {
        JsonNode models = installedModels();
        Map<String, Object> status = object(
            "localModel", hasModel(models, localModel) ? "online" : "offline",
            "localModelName", localModel,
            "embedding", hasModel(models, embeddingModel) ? "online" : "offline",
            "externalApi", !externalEnabled ? "disabled" : externalAvailable() ? "configured" : "not_configured",
            "vectorDb", vectorAvailable() ? "online" : "offline"
        );
        return status;
    }

    String localName() {
        return localModel;
    }

    boolean localAvailable() {
        return hasModel(installedModels(), localModel);
    }

    boolean externalAvailable() {
        return externalEnabled && (present(deepseekKey()) || present(glmKey()));
    }

    /** 主模型密钥：配置 → 系统属性 → 环境变量 → 本地 ai-keys.txt（与 MesAiController 一致）。 */
    String deepseekKey() {
        return resolveKey(deepseekKey, "MES_AI_KEY");
    }

    /** 兜底模型密钥。 */
    String glmKey() {
        return resolveKey(glmKey, "MES_AI_FB_KEY");
    }

    private static String resolveKey(String configured, String name) {
        if (present(configured)) return configured.trim();
        String value = System.getProperty(name);
        if (present(value)) return value.trim();
        value = System.getenv(name);
        if (present(value)) return value.trim();
        for (String path : new String[]{"ai-keys.txt", "E:/JavaCode/ai-keys.txt"}) {
            try {
                java.io.File file = new java.io.File(path);
                if (!file.isFile()) continue;
                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int index = line.indexOf('=');
                        if (index > 0 && line.substring(0, index).trim().equals(name)) {
                            String value2 = line.substring(index + 1).trim();
                            if (!value2.isEmpty()) return value2;
                        }
                    }
                }
            } catch (IOException ignored) {
                // 读取失败时继续尝试下一个来源
            }
        }
        return "";
    }

    List<ToolChannel> toolChannels(boolean allowExternal, boolean skipLocal) {
        List<ToolChannel> choices = new ArrayList<>();
        if (!skipLocal && localAvailable()) {
            choices.add(new ToolChannel(address(ollama, "/v1/chat/completions"), localModel, null));
        }
        if (allowExternal && externalEnabled) {
            if (present(deepseekKey())) choices.add(new ToolChannel(deepseekUrl, deepseekModel, deepseekKey()));
            if (present(glmKey())) choices.add(new ToolChannel(glmUrl, glmModel, glmKey()));
        }
        return choices;
    }

    final class ToolChannel {
        private final String url;
        final String model;
        private final String key;

        ToolChannel(String url, String model, String key) {
            this.url = url;
            this.model = model;
            this.key = key;
        }

        boolean isExternal() {
            return key != null;
        }

        JsonNode plan(List<Map<String, Object>> conversation, List<Map<String, Object>> tools, int timeout)
                throws IOException {
            return request("POST", url, object("model", model, "messages", conversation, "tools", tools,
                "tool_choice", "auto", "temperature", 0, "max_tokens", 350, "stream", false),
                isExternal() ? "Authorization" : null, isExternal() ? "Bearer " + key : null, timeout)
                .path("choices").path(0).path("message");
        }

        String summarize(String system, String prompt) throws IOException {
            return request("POST", url, object("model", model, "messages", messages(system, prompt),
                "temperature", 0, "max_tokens", 220, "stream", false),
                isExternal() ? "Authorization" : null, isExternal() ? "Bearer " + key : null, 7000)
                .path("choices").path(0).path("message").path("content").asText("").trim();
        }
    }

    boolean vectorAvailable() {
        try {
            return qdrant("GET", "/", null, 1200).path("title").asText().startsWith("qdrant");
        } catch (IOException e) {
            return false;
        }
    }

    boolean indexingAvailable() {
        return vectorAvailable() && hasModel(installedModels(), embeddingModel);
    }

    JsonNode embed(List<String> texts) throws IOException {
        JsonNode vectors = request("POST", address(ollama, "/api/embed"), object(
            "model", embeddingModel, "input", texts, "truncate", true,
            "keep_alive", "5m", "options", object("num_gpu", 0)
        ), null, null, 90000).path("embeddings");
        if (!vectors.isArray() || vectors.size() != texts.size() || vectors.get(0).size() == 0) {
            throw new IOException("Embedding service returned incomplete vectors");
        }
        return vectors;
    }

    String collectionPath() {
        return "/collections/" + collection;
    }

    JsonNode qdrant(String method, String path, Object body, int readTimeout) throws IOException {
        return request(method, address(qdrant, path), body, "api-key", qdrantKey, readTimeout);
    }

    String generateLocal(String system, String prompt, TextSink sink, BooleanSupplier cancelled) throws IOException {
        Object body = object("model", localModel, "messages", messages(system, prompt), "stream", true,
            "keep_alive", -1, "options", object("num_ctx", 2048, "num_predict", 512, "temperature", 0.2));
        generate(address(ollama, "/api/chat"), body, null, true, sink, cancelled);
        return localModel;
    }

    String generateExternal(String system, String prompt, TextSink sink, BooleanSupplier cancelled) throws IOException {
        IOException failure = new IOException("External model unavailable");
        for (String[] channel : new String[][]{{deepseekUrl, deepseekKey(), deepseekModel}, {glmUrl, glmKey(), glmModel}}) {
            if (!externalEnabled || !present(channel[1]) || cancelled.getAsBoolean()) {
                continue;
            }
            try {
                Object body = object("model", channel[2], "messages", messages(system, prompt),
                    "stream", true, "temperature", 0.2, "max_tokens", 512);
                generate(channel[0], body, channel[1], false, sink, cancelled);
                return channel[2];
            } catch (PartialOutputException e) {
                throw e;
            } catch (IOException e) {
                failure = e;
            }
        }
        throw failure;
    }

    /** 自然语言回答：外部模型优先，其次本地模型；两者都不可用时抛错，由调用方保留兜底话术。 */
    String generateNatural(String system, String prompt, TextSink sink, BooleanSupplier cancelled) throws IOException {
        IOException failure = new IOException("Model unavailable");
        if (externalAvailable()) {
            try {
                return generateExternal(system, prompt, sink, cancelled);
            } catch (PartialOutputException e) {
                throw e;
            } catch (IOException e) {
                failure = e;
            }
        }
        if (localAvailable()) {
            try {
                return generateLocal(system, prompt, sink, cancelled);
            } catch (IOException e) {
                failure = e;
            }
        }
        throw failure;
    }

    /** 按本轮授权选择渠道：允许外部时外部优先，否则只走本地模型。 */
    String generateFor(boolean allowExternal, String system, String prompt, TextSink sink,
                       BooleanSupplier cancelled) throws IOException {
        if (allowExternal && externalAvailable()) {
            return generateExternal(system, prompt, sink, cancelled);
        }
        if (localAvailable()) {
            return generateLocal(system, prompt, sink, cancelled);
        }
        throw new IOException("Model unavailable");
    }

    /** 一次性取得完整回答文本；模型不可用时返回空串。 */
    public String naturalText(boolean allowExternal, String system, String prompt) {
        StringBuilder text = new StringBuilder();
        try {
            generateFor(allowExternal, system, prompt, part -> text.append(part), () -> false);
        } catch (IOException e) {
            return "";
        }
        return text.toString().trim();
    }

    public String naturalText(String system, String prompt) {
        return naturalText(true, system, prompt);
    }

    private void generate(String url, Object body, String apiKey, boolean local,
                          TextSink sink, BooleanSupplier cancelled) throws IOException {
        HttpURLConnection connection = connect("POST", url, apiKey == null ? null : "Authorization",
            apiKey == null ? null : "Bearer " + apiKey, 60000);
        boolean emitted = false;
        boolean finished = false;
        try {
            writeBody(connection, body);
            validateResponse(connection);
            try (BufferedReader reader = reader(connection)) {
                String line;
                while (!cancelled.getAsBoolean() && (line = reader.readLine()) != null) {
                    if (line.isEmpty() || (!local && !line.startsWith("data:"))) {
                        continue;
                    }
                    String payload = local ? line : line.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        finished = true;
                        break;
                    }
                    JsonNode item = json.readTree(payload);
                    if (item.hasNonNull("error")) {
                        throw new IOException("Model returned an error");
                    }
                    JsonNode choice = item.path("choices").path(0);
                    String delta = local ? item.path("message").path("content").asText("")
                        : choice.path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        emitted = true;
                        sink.accept(delta);
                    }
                    if (local ? item.path("done").asBoolean() : choice.hasNonNull("finish_reason")) {
                        finished = true;
                        break;
                    }
                }
            }
            if (!cancelled.getAsBoolean() && (!emitted || !finished)) {
                throw new IOException("Model output was empty or incomplete");
            }
        } catch (IOException e) {
            if (emitted) {
                throw new PartialOutputException(e);
            }
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    private JsonNode installedModels() {
        try {
            return request("GET", address(ollama, "/api/tags"), null, null, null, 1200).path("models");
        } catch (IOException e) {
            return json.createArrayNode();
        }
    }

    private static boolean hasModel(JsonNode models, String name) {
        String tagged = name.contains(":") ? name : name + ":latest";
        for (JsonNode model : models) {
            if (tagged.equals(model.path("name").asText()) || tagged.equals(model.path("model").asText())) {
                return true;
            }
        }
        return false;
    }

    JsonNode request(String method, String url, Object body, String header, String value,
                             int timeout) throws IOException {
        HttpURLConnection connection = connect(method, url, header, value, timeout);
        try {
            if (body != null) {
                writeBody(connection, body);
            }
            validateResponse(connection);
            try (BufferedReader result = reader(connection)) {
                return json.readTree(result);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection connect(String method, String url, String header, String value,
                                             int timeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(Math.min(2500, timeout));
        connection.setReadTimeout(timeout);
        if (header != null && present(value)) {
            connection.setRequestProperty(header, value);
        }
        return connection;
    }

    private void writeBody(HttpURLConnection connection, Object body) throws IOException {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            json.writeValue(output, body);
        }
    }

    private static BufferedReader reader(HttpURLConnection connection) throws IOException {
        return new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
    }

    private static void validateResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new HttpFailure(status);
        }
    }

    static Map<String, Object> object(Object... entries) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            fields.put((String) entries[i], entries[i + 1]);
        }
        return fields;
    }

    private static List<Map<String, Object>> messages(String system, String prompt) {
        return Arrays.asList(object("role", "system", "content", system), object("role", "user", "content", prompt));
    }

    private static String address(String base, String path) {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) + path : base + path;
    }

    private static boolean present(String value) {
        return value != null && !value.trim().isEmpty();
    }

    interface TextSink {
        void accept(String content) throws IOException;
    }

    static class HttpFailure extends IOException {
        final int status;

        HttpFailure(int status) {
            super("Upstream HTTP " + status);
            this.status = status;
        }
    }

    static class PartialOutputException extends IOException {
        PartialOutputException(IOException cause) {
            super(cause);
        }
    }
}
