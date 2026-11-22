package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ktg.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.ktg.mes.ai.service.KbRagEngines.object;

/**
 * MES 与 Dify 的分阶段编排适配器。
 *
 * <p>一轮问答在 Dify 中依次经过「问题改写 → 检索分支 → 证据排序 → 回答生成 → 引用整理 → 完成回执」，
 * 每个阶段由一个 HTTP 节点回调 {@code POST /mes/ai/dify/callback} 触发。MES 在回调中执行该阶段并签发
 * 下一步凭证：凭证只对固定阶段有效、只可使用一次、绑定本轮运行与提问账号，过期或阶段不符直接拒绝。
 *
 * <p>敏感内容（问题原文、文档片段、图谱、业务数据行、回答正文、登录令牌）不进入 Dify：请求只带一次性
 * 凭证和分支标识，回执只带完成凭证。检索、计算与生成全部在 MES 完成，Dify 只负责选择分支与推进步骤。
 *
 * <p>Dify 未配置、不可达、回调被拒或完成回执校验失败时，本轮退化为 MES 既有问答流程，已执行的阶段结果
 * 直接复用，不重复查询。
 */
@Service
public class AiDifyWorkflow {
    private static final Logger log = LoggerFactory.getLogger(AiDifyWorkflow.class);
    /** 阶段标识与 docs/dify/mes-ai-workflow.yml 中的 HTTP 节点一一对应。 */
    public static final String REWRITE = "rewrite";
    public static final String DOC = "doc";
    public static final String GRAPH = "graph";
    public static final String BUSINESS = "business";
    public static final String MIXED_DOC = "mixed_doc";
    public static final String MIXED_BUSINESS = "mixed_business";
    public static final String RANK = "rank";
    public static final String GENERATE = "generate";
    public static final String CITATIONS = "citations";
    public static final String FINALIZE = "finalize";

    public static final String ROUTE_DOC = "doc";
    public static final String ROUTE_GRAPH = "graph";
    public static final String ROUTE_BUSINESS = "business";
    public static final String ROUTE_MIXED = "mixed";

    private static final List<String> GATES = Arrays.asList(RANK, GENERATE, CITATIONS, FINALIZE);
    private static final String TICKET_PATTERN = "[A-Za-z0-9_-]{43}";
    private static final String RUN_ID_PATTERN = "[A-Za-z0-9_-]{1,64}";
    private static final int DEFAULT_TIMEOUT_MS = 12000;
    /** 请求超时后仍接受回调的余量，避免 Dify 稍慢一步就被判为失败。 */
    private static final int EXPIRY_GRACE_MS = 8000;

    private final AiBusinessAnswer business;
    private final KbRagEngines http;
    private final String endpoint;
    private final String apiKey;
    private final int timeoutMs;
    private final SecureRandom random = new SecureRandom();
    private final Semaphore slots = new Semaphore(8);
    private final ConcurrentMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private volatile String connectionState = "configured";
    private volatile String lastRunId = "";
    private volatile String lastVerifiedAt = "";
    private volatile String lastDetail = "";

    public AiDifyWorkflow(AiBusinessAnswer business, KbRagEngines http,
                          @Value("${mes.ai.dify.api-base-url:}") String apiBaseUrl,
                          @Value("${mes.ai.dify.api-key:}") String apiKey,
                          @Value("${mes.ai.dify.timeout-ms:12000}") String timeout) {
        this.business = business;
        this.http = http;
        String base = apiBaseUrl == null ? "" : apiBaseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = base.isEmpty() || this.apiKey.isEmpty() ? null : base.replaceAll("/+$", "") + "/workflows/run";
        this.timeoutMs = timeout(timeout);
    }

    static int timeout(String configured) {
        try {
            return Math.max(3000, Math.min(60000, Integer.parseInt(configured.trim())));
        } catch (NumberFormatException | NullPointerException e) {
            return DEFAULT_TIMEOUT_MS;
        }
    }

    /** 助手状态入口：未配置、已配置待验证、在线、离线。 */
    public String status() {
        return endpoint == null ? "not_configured" : connectionState;
    }

    /** 状态明细，只包含执行元数据，不包含密钥与凭证。 */
    public Map<String, Object> statusDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("state", status());
        detail.put("configured", endpoint != null);
        detail.put("timeoutMs", timeoutMs);
        detail.put("lastRunId", lastRunId);
        detail.put("verifiedAt", lastVerifiedAt);
        detail.put("detail", lastDetail);
        return detail;
    }

    /**
     * 业务数据分支：规则命中时由 Dify 推进阶段，否则沿用既有问答。
     */
    public Map<String, Object> answer(String question, boolean allowExternal, boolean skipLocal) {
        AiBusinessRules.Call rule = endpoint == null ? null : business.findRule(question);
        if (rule == null) {
            return business.answer(question, allowExternal, skipLocal);
        }
        long started = System.currentTimeMillis();
        List<Map<String, Object>> trace = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
        try {
            RunResult<Map<String, Object>> run = dispatch(ROUTE_BUSINESS,
                Collections.singletonList(new Leg(BUSINESS,
                    () -> business.answerFromRule(rule, started, trace, "MES 只读工具规划", "以提问账号的权限执行"))),
                values -> asAnswer(values.get(0)), businessDetail(), started, trace::add);
            Map<String, Object> answer = run.value;
            answer.put("trace", new ArrayList<>(trace));
            return answer;
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asAnswer(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * 按路由推进一轮工作流。检索分支的 {@link Leg} 只在对应阶段的回调中执行一次；Dify 未参与或失败时
     * 由调用线程原地执行同一任务，结果与回调路径完全一致。
     */
    public <T> RunResult<T> dispatch(String route, List<Leg> legs, Function<List<Object>, T> combine,
                                     Detail detail, long startedAt, Trace trace) throws IOException {
        if (endpoint == null) {
            List<Object> values = new ArrayList<>();
            for (Leg leg : legs) {
                values.add(runDirect(leg.action));
            }
            return new RunResult<>(combine.apply(values), null, null, null, route);
        }
        if (!slots.tryAcquire()) {
            String reason = "工作流正忙，本轮由 MES 直接执行";
            emit(trace, "Dify 兜底", reason, startedAt, 0L);
            return new RunResult<>(combine.apply(runDirect(legs)), "Dify 兜底", reason, null, route);
        }
        try {
            return execute(route, legs, combine, detail, startedAt, trace);
        } finally {
            slots.release();
        }
    }

    private <T> RunResult<T> execute(String route, List<Leg> legs, Function<List<Object>, T> combine,
                                     Detail detail, long startedAt, Trace trace) throws IOException {
        Run run = new Run(route, legs, detail, trace, startedAt);
        String first = issue(run, REWRITE);
        JsonNode response = null;
        String failure = null;
        try {
            response = http.request("POST", endpoint, object("inputs", object("ticket", first, "route", route),
                "response_mode", "blocking", "user", "mes-assistant"), "Authorization", "Bearer " + apiKey, timeoutMs);
        } catch (IOException e) {
            failure = "Dify 不可达或等待超时";
        }
        String status = "";
        String runId = "";
        String receipt = "";
        String echoedRoute = "";
        if (response != null) {
            JsonNode data = response.path("data");
            status = data.path("status").asText("");
            runId = response.path("workflow_run_id").asText("");
            receipt = data.path("outputs").path("receipt").asText("");
            echoedRoute = data.path("outputs").path("route").asText("");
        }
        boolean verified = response != null && "succeeded".equals(status)
            && runId.matches(RUN_ID_PATTERN) && run.receipt != null && run.receipt.equals(receipt)
            && (echoedRoute.isEmpty() || route.equals(echoedRoute));
        if (!verified && failure == null) {
            failure = failureReason(status, runId, receipt, echoedRoute, run, route);
        }
        run.close();
        T value = combine.apply(run.collect());
        long elapsed = System.currentTimeMillis() - startedAt;
        if (verified) {
            connectionState = "online";
            lastRunId = runId;
            lastVerifiedAt = Instant.now().toString();
            lastDetail = "运行 " + runId + " · 分支 " + routeLabel(route);
            log.info("[Dify] 编排校验通过 route={} runId={} stages={} elapsedMs={}",
                route, runId, run.stages.size(), elapsed);
            run.emit("Dify 编排", lastDetail, elapsed);
            return new RunResult<>(value, "Dify 编排", lastDetail, runId, route);
        }
        connectionState = "offline";
        lastRunId = runId.matches(RUN_ID_PATTERN) ? runId : "";
        lastDetail = failure;
        log.warn("[Dify] 本轮退化 route={} reason={} runId={} rejection={} elapsedMs={}",
            route, failure, lastRunId, run.rejection, elapsed);
        run.emit("Dify 兜底", failure + "，本轮由 MES 直接执行", elapsed);
        return new RunResult<>(value, "Dify 兜底", failure, null, route);
    }

    private static String failureReason(String status, String runId, String receipt, String echoedRoute,
                                        Run run, String route) {
        if (!"succeeded".equals(status)) {
            return run.rejection == null ? "Dify 工作流未返回成功状态" : "Dify 阶段回执被拒绝";
        }
        if (!runId.matches(RUN_ID_PATTERN)) {
            return "Dify 未返回可校验的运行编号";
        }
        if (run.receipt == null) {
            return "Dify 未完成全部阶段";
        }
        if (!run.receipt.equals(receipt)) {
            return "Dify 完成回执未通过校验";
        }
        return "Dify 回执分支与请求分支不一致（" + echoedRoute + "）";
    }

    /**
     * 阶段回调。ticket 只能使用一次，且必须与本轮当前待执行阶段一致。
     *
     * @return 通过时响应体为下一阶段凭证（最后一步为完成回执），否则 403 且响应体不含任何执行信息。
     */
    public Ack callback(String ticket, String stage) {
        if (ticket == null || !ticket.matches(TICKET_PATTERN) || stage == null || stage.isEmpty()) {
            return Ack.rejected(stage);
        }
        Ticket entry = tickets.remove(ticket);
        if (entry == null) {
            return Ack.rejected(stage);
        }
        Run run = entry.run;
        if (run.closed) {
            return reject(run, "run_closed");
        }
        if (System.nanoTime() - run.expiresAt >= 0) {
            return reject(run, "ticket_expired");
        }
        if (!entry.stage.equals(stage)) {
            return reject(run, "stage_mismatch");
        }
        synchronized (run.lock) {
            int index = run.next.get();
            if (run.closed || index >= run.stages.size() || !run.stages.get(index).equals(stage)) {
                return reject(run, "stage_order");
            }
            long stageStarted = System.currentTimeMillis();
            try {
                run.execute(stage);
            } catch (IOException | RuntimeException e) {
                return reject(run, "stage_failed");
            }
            run.next.incrementAndGet();
            run.record(stage, stageStarted);
            if (run.closed) {
                return reject(run, "run_closed");
            }
            if (run.next.get() >= run.stages.size()) {
                return Ack.ok(run.receipt, stage, FINALIZE, run.route);
            }
            String next = run.stages.get(run.next.get());
            return Ack.ok(issue(run, next), stage, next, run.route);
        }
    }

    private Ack reject(Run run, String reason) {
        run.rejection = reason;
        return Ack.rejected(run.currentStage());
    }

    private String issue(Run run, String stage) {
        String value = token();
        run.issued.add(value);
        tickets.put(value, new Ticket(run, stage));
        return value;
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Object runDirect(Callable<?> action) throws IOException {
        try {
            return action.call();
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("本轮 MES 任务未完成。", e);
        }
    }

    private static List<Object> runDirect(List<Leg> legs) throws IOException {
        List<Object> values = new ArrayList<>();
        for (Leg leg : legs) {
            values.add(runDirect(leg.action));
        }
        return values;
    }

    private static Object demand(FutureTask<Object> task) throws IOException {
        task.run();
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("本轮 MES 任务已中断。", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("本轮 MES 任务未完成。", cause);
        }
    }

    private static void emit(Trace trace, String label, String detail, long startedAt, long durationMs) {
        if (trace == null) {
            return;
        }
        try {
            trace.accept(entry(label, detail, System.currentTimeMillis() - startedAt, durationMs));
        } catch (IOException | RuntimeException ignored) {
            // 轨迹只是附加信息；断线由调用方自己的停止标志处理。
        }
    }

    static Map<String, Object> entry(String label, String detail, long elapsedMs, long durationMs) {
        return object("label", label, "detail", detail, "elapsedMs", elapsedMs, "durationMs", durationMs);
    }

    static String routeLabel(String route) {
        if (ROUTE_DOC.equals(route)) {
            return "文档";
        }
        if (ROUTE_GRAPH.equals(route)) {
            return "图谱";
        }
        if (ROUTE_BUSINESS.equals(route)) {
            return "业务数据";
        }
        if (ROUTE_MIXED.equals(route)) {
            return "文档＋业务数据";
        }
        return route;
    }

    static String stageLabel(String stage) {
        switch (stage) {
            case REWRITE: return "问题改写";
            case DOC: return "文档检索";
            case GRAPH: return "图谱检索";
            case BUSINESS: return "业务工具查询";
            case MIXED_DOC: return "混合问答·文档检索";
            case MIXED_BUSINESS: return "混合问答·业务查询";
            case RANK: return "证据排序";
            case GENERATE: return "回答生成";
            case CITATIONS: return "引用整理";
            case FINALIZE: return "完成回执";
            default: return stage;
        }
    }

    /** 业务数据分支各阶段在轨迹中的说明。 */
    static Detail businessDetail() {
        return (stage, legs) -> {
            switch (stage) {
                case REWRITE: return "分支 业务数据";
                case BUSINESS: return "按提问账号权限执行只读工具";
                case RANK: return "无需重排（业务表格保持原始口径）";
                case GENERATE: return "规则表格与确定性汇总，数字由 MES 计算";
                case CITATIONS: return "业务分支不生成文档引用";
                default: return "完成回执，交由 MES 展示答案";
            }
        };
    }

    /** 一次工作流中的检索分支。同一 Leg 无论由回调线程还是请求线程触发，都只执行一次。 */
    public static final class Leg {
        private final String stage;
        private final Callable<?> action;

        public Leg(String stage, Callable<?> action) {
            this.stage = stage;
            this.action = action;
        }
    }

    /** 阶段说明。用于把实际策略写进 MES 执行轨迹。 */
    public interface Detail {
        String of(String stage, List<Object> completedLegResults);
    }

    /** 轨迹出口。RAG 走 SSE，业务问答写入响应体的 trace。 */
    public interface Trace {
        void accept(Map<String, Object> entry) throws IOException;
    }

    public static final class RunResult<T> {
        public final T value;
        public final String label;
        public final String detail;
        public final String runId;
        public final String route;

        RunResult(T value, String label, String detail, String runId, String route) {
            this.value = value;
            this.label = label;
            this.detail = detail;
            this.runId = runId;
            this.route = route;
        }
    }

    /** 回调结果：只有握手成功才返回凭证，拒绝时不泄露原因。 */
    public static final class Ack {
        private final boolean accepted;
        private final String body;
        private final String stage;
        private final String nextStage;
        private final String route;

        private Ack(boolean accepted, String body, String stage, String nextStage, String route) {
            this.accepted = accepted;
            this.body = body;
            this.stage = stage == null ? "" : stage;
            this.nextStage = nextStage == null ? "" : nextStage;
            this.route = route == null ? "" : route;
        }

        static Ack ok(String body, String stage, String nextStage, String route) {
            return new Ack(true, body == null ? "" : body, stage, nextStage, route);
        }

        static Ack rejected(String stage) {
            return new Ack(false, "", stage, "", "");
        }

        public boolean accepted() {
            return accepted;
        }

        public String body() {
            return body;
        }

        /** 已完成的阶段。 */
        public String stage() {
            return stage;
        }

        /** 下一步凭证对应的阶段，最后一步为 finalize。 */
        public String nextStage() {
            return nextStage;
        }

        /** 本轮分支标识。 */
        public String route() {
            return route;
        }
    }

    private static final class Ticket {
        private final Run run;
        private final String stage;

        Ticket(Run run, String stage) {
            this.run = run;
            this.stage = stage;
        }
    }

    /** 一轮运行的全部状态只保留在 MES 内存中，流程结束、取消或超时后释放。 */
    private final class Run {
        final Object lock = new Object();
        final String route;
        final long startedAt;
        final long expiresAt;
        final Detail detail;
        final Trace trace;
        final List<String> stages = new ArrayList<>();
        final Map<String, Integer> legIndex = new LinkedHashMap<>();
        final List<FutureTask<Object>> tasks = new ArrayList<>();
        final List<Object> completed = Collections.synchronizedList(new ArrayList<Object>());
        final Set<String> issued = ConcurrentHashMap.newKeySet();
        final AtomicInteger next = new AtomicInteger();
        volatile String receipt;
        volatile String rejection;
        volatile boolean closed;

        Run(String route, List<Leg> legs, Detail detail, Trace trace, long startedAt) {
            this.route = route;
            this.startedAt = startedAt;
            this.detail = detail;
            this.trace = trace;
            this.expiresAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs + EXPIRY_GRACE_MS);
            Authentication user = SecurityContextHolder.getContext().getAuthentication();
            stages.add(REWRITE);
            for (Leg leg : legs) {
                if (legIndex.containsKey(leg.stage) || GATES.contains(leg.stage) || REWRITE.equals(leg.stage)) {
                    throw new ServiceException("重复或保留的 Dify 阶段：" + leg.stage);
                }
                legIndex.put(leg.stage, tasks.size());
                stages.add(leg.stage);
                tasks.add(new FutureTask<>(asUser(user, leg.action)));
            }
            stages.addAll(GATES);
        }

        void execute(String stage) throws IOException {
            Integer index = legIndex.get(stage);
            if (index == null) {
                if (FINALIZE.equals(stage)) {
                    receipt = token();
                }
                return;
            }
            completed.add(demand(tasks.get(index)));
        }

        List<Object> collect() throws IOException {
            List<Object> values = new ArrayList<>();
            for (FutureTask<Object> task : tasks) {
                values.add(demand(task));
            }
            return values;
        }

        void record(String stage, long stageStarted) {
            long durationMs = System.currentTimeMillis() - stageStarted;
            String description;
            try {
                description = detail == null ? null : detail.of(stage, completed);
            } catch (RuntimeException e) {
                description = null;
            }
            if (description == null || description.isEmpty()) {
                description = stageLabel(stage) + "已完成";
            }
            emit("Dify · " + stageLabel(stage), description + " · " + durationMs + " ms",
                System.currentTimeMillis() - startedAt, durationMs);
        }

        String currentStage() {
            int index = next.get();
            return index < stages.size() ? stages.get(index) : FINALIZE;
        }

        void emit(String label, String description, long elapsedMs) {
            emit(label, description, elapsedMs, 0L);
        }

        void emit(String label, String description, long elapsedMs, long durationMs) {
            AiDifyWorkflow.emit(trace, label, description, startedAt, durationMs);
        }

        void close() {
            synchronized (lock) {
                closed = true;
                for (String ticket : issued) {
                    tickets.remove(ticket);
                }
            }
        }

        private Callable<Object> asUser(Authentication user, Callable<?> action) {
            return () -> {
                SecurityContext previous = SecurityContextHolder.getContext();
                if (user == null) {
                    return action.call();
                }
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(user);
                SecurityContextHolder.setContext(context);
                try {
                    return action.call();
                } finally {
                    SecurityContextHolder.setContext(previous);
                }
            };
        }
    }
}
