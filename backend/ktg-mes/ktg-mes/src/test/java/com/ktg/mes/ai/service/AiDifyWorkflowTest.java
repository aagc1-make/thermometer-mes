package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ktg.common.exception.ServiceException;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AiDifyWorkflowTest {
    private final ObjectMapper json = new ObjectMapper();

    private static final String BASE_URL = "http://dify.local/v1";
    private static final String APP_KEY = "app-test-key";
    private static final String TIMEOUT = "5000";

    private static List<String> plan(String route) {
        List<String> stages = new ArrayList<>();
        stages.add(AiDifyWorkflow.REWRITE);
        if (AiDifyWorkflow.ROUTE_MIXED.equals(route)) {
            stages.add(AiDifyWorkflow.MIXED_DOC);
            stages.add(AiDifyWorkflow.MIXED_BUSINESS);
        } else {
            stages.add(route);
        }
        stages.add(AiDifyWorkflow.RANK);
        stages.add(AiDifyWorkflow.GENERATE);
        stages.add(AiDifyWorkflow.CITATIONS);
        stages.add(AiDifyWorkflow.FINALIZE);
        return stages;
    }

    private static AiDifyWorkflow.Leg leg(String stage, String value, AtomicInteger runs) {
        return new AiDifyWorkflow.Leg(stage, () -> {
            runs.incrementAndGet();
            return value;
        });
    }

    @Test
    public void documentRouteCompletesEveryStageOnce() throws IOException {
        FakeDify dify = new FakeDify(json);
        AiDifyWorkflow workflow = dify.workflow();
        ReflectionTestUtils.setField(workflow, "consoleWorkflowUrl", "http://dify.local/app/mes/workflow");
        AtomicInteger runs = new AtomicInteger();
        List<Map<String, Object>> trace = new ArrayList<>();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), trace::add);

        assertEquals("Dify 编排", run.label);
        assertEquals("run_abc123", run.runId);
        assertEquals("evidence", run.value);
        assertEquals(1, runs.get());
        assertEquals(plan(AiDifyWorkflow.ROUTE_DOC), dify.stages);
        assertEquals("online", workflow.status());
        assertEquals("run_abc123", workflow.statusDetail().get("lastRunId"));
        assertTrue(trace.get(0).get("label").toString().contains("问题改写"));
        assertEquals("Dify 编排", trace.get(trace.size() - 1).get("label"));
        assertEquals("run_abc123", trace.get(trace.size() - 1).get("runId"));
        assertEquals("http://dify.local/app/mes/workflow?runId=run_abc123",
            trace.get(trace.size() - 1).get("runUrl"));
        assertTrue(trace.get(trace.size() - 1).get("detail").toString().contains("运行 run_abc123"));
        assertTrue("阶段耗时写进轨迹", trace.get(1).get("detail").toString().contains(" ms"));
        assertEquals("rewrite", dify.acks.get(0).stage());
        assertEquals(AiDifyWorkflow.DOC, dify.acks.get(0).nextStage());
        assertEquals(AiDifyWorkflow.ROUTE_DOC, dify.acks.get(0).route());
    }

    @Test
    public void graphRouteUsesGraphStage() throws IOException {
        FakeDify dify = new FakeDify(json);
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_GRAPH,
            Collections.singletonList(leg(AiDifyWorkflow.GRAPH, "graph", runs)), values -> values.get(0),
            KbRagService.detail(true, false), System.currentTimeMillis(), entry -> { });

        assertEquals("Dify 编排", run.label);
        assertTrue(dify.stages.contains(AiDifyWorkflow.GRAPH));
        assertEquals(1, runs.get());
    }

    @Test
    public void mixedRouteKeepsBothLegsUnderOneRun() throws IOException {
        FakeDify dify = new FakeDify(json);
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger documents = new AtomicInteger();
        AtomicInteger rows = new AtomicInteger();

        AiDifyWorkflow.RunResult<List<Object>> run = workflow.dispatch(AiDifyWorkflow.ROUTE_MIXED,
            Arrays.asList(new AiDifyWorkflow.Leg(AiDifyWorkflow.MIXED_DOC, () -> {
                documents.incrementAndGet();
                return "citations";
            }), new AiDifyWorkflow.Leg(AiDifyWorkflow.MIXED_BUSINESS, () -> {
                rows.incrementAndGet();
                return "table";
            })), values -> new ArrayList<>(values), KbRagService.detail(false, true),
            System.currentTimeMillis(), entry -> { });

        assertEquals("Dify 编排", run.label);
        assertEquals(Arrays.asList("citations", "table"), run.value);
        assertEquals(1, documents.get());
        assertEquals(1, rows.get());
        assertEquals(Arrays.asList("rewrite", "mixed_doc", "mixed_business", "rank", "generate", "citations",
            "finalize"), dify.stages);
    }

    @Test
    public void consumedTicketCannotBeReplayed() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.probeReplay = true;
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), entry -> { });

        assertEquals("Dify 编排", run.label);
        assertEquals(1, runs.get());
        assertEquals(1, dify.probes.size());
        assertFalse("重复使用已消费的凭证必须被拒绝", dify.probes.get(0).accepted());
    }

    @Test
    public void wrongStageWithValidTicketIsRejected() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.mismatchFirst = true;
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), entry -> { });

        assertEquals("Dify 兜底", run.label);
        assertEquals("evidence", run.value);
        assertEquals("被拒绝的阶段不执行任务", 1, runs.get());
        assertFalse(dify.probes.get(0).accepted());
        assertTrue(run.detail.contains("阶段回执被拒绝"));
    }

    @Test
    public void outOfOrderStageFallsBackAndReusesFinishedWork() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.disorder = true;
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();
        List<Map<String, Object>> trace = new ArrayList<>();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), trace::add);

        assertEquals("Dify 兜底", run.label);
        assertEquals("evidence", run.value);
        assertEquals("已完成阶段不重复执行", 1, runs.get());
        assertEquals("offline", workflow.status());
        assertEquals("Dify 兜底", trace.get(trace.size() - 1).get("label"));
    }

    @Test
    public void mismatchedCompletionReceiptFallsBack() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.receipt = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_BUSINESS,
            Collections.singletonList(leg(AiDifyWorkflow.BUSINESS, "answer", runs)), values -> values.get(0),
            AiDifyWorkflow.businessDetail(), System.currentTimeMillis(), entry -> { });

        assertEquals("Dify 兜底", run.label);
        assertNull(run.runId);
        assertTrue(run.detail.contains("完成回执"));
        assertEquals(1, runs.get());
    }

    @Test
    public void failedWorkflowStatusFallsBack() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.status = "failed";
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), entry -> { });

        assertEquals("Dify 兜底", run.label);
        assertTrue(run.detail.contains("成功状态"));
        assertEquals(1, runs.get());
    }

    @Test
    public void unreachableDifyFallsBackToMesPath() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.offline = true;
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();
        List<Map<String, Object>> trace = new ArrayList<>();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), trace::add);

        assertEquals("Dify 兜底", run.label);
        assertTrue(run.detail.contains("不可达"));
        assertEquals("evidence", run.value);
        assertEquals(1, runs.get());
        assertEquals(1, trace.size());
        assertEquals("offline", workflow.status());
    }

    @Test
    public void unconfiguredDifyKeepsExistingPath() throws IOException {
        FakeDify dify = new FakeDify(json);
        AiDifyWorkflow workflow = new AiDifyWorkflow(null, dify, "", "", TIMEOUT);
        AtomicInteger runs = new AtomicInteger();
        List<Map<String, Object>> trace = new ArrayList<>();

        AiDifyWorkflow.RunResult<Object> run = workflow.dispatch(AiDifyWorkflow.ROUTE_DOC,
            Collections.singletonList(leg(AiDifyWorkflow.DOC, "evidence", runs)), values -> values.get(0),
            KbRagService.detail(false, false), System.currentTimeMillis(), trace::add);

        assertNull(run.label);
        assertEquals("evidence", run.value);
        assertEquals(1, runs.get());
        assertTrue(trace.isEmpty());
        assertEquals("not_configured", workflow.status());
        assertTrue(dify.stages.isEmpty());
    }

    @Test
    public void stateRecoversAfterOneSuccessfulRun() throws IOException {
        FakeDify dify = new FakeDify(json);
        dify.offline = true;
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();

        workflow.dispatch(AiDifyWorkflow.ROUTE_DOC, Collections.singletonList(leg(AiDifyWorkflow.DOC, "a", runs)),
            values -> values.get(0), KbRagService.detail(false, false), System.currentTimeMillis(), entry -> { });
        assertEquals("offline", workflow.status());

        dify.offline = false;
        workflow.dispatch(AiDifyWorkflow.ROUTE_DOC, Collections.singletonList(leg(AiDifyWorkflow.DOC, "b", runs)),
            values -> values.get(0), KbRagService.detail(false, false), System.currentTimeMillis(), entry -> { });
        assertEquals("online", workflow.status());
        assertEquals(2, runs.get());
    }

    @Test
    public void duplicateStageIsRejectedBeforeRunning() {
        FakeDify dify = new FakeDify(json);
        AiDifyWorkflow workflow = dify.workflow();
        AtomicInteger runs = new AtomicInteger();
        try {
            workflow.dispatch(AiDifyWorkflow.ROUTE_DOC, Arrays.asList(leg(AiDifyWorkflow.DOC, "a", runs),
                leg(AiDifyWorkflow.DOC, "b", runs)), values -> values.get(0),
                KbRagService.detail(false, false), System.currentTimeMillis(), entry -> { });
            throw new AssertionError("重复阶段必须被拒绝");
        } catch (ServiceException expected) {
            assertEquals(0, runs.get());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /** 模拟 Dify：按工作流顺序回调 MES，并用真实的回执结构返回。 */
    private static final class FakeDify extends KbRagEngines {
        private final ObjectMapper mapper;
        private final List<String> stages = new ArrayList<>();
        private final List<AiDifyWorkflow.Ack> probes = new ArrayList<>();
        private final List<AiDifyWorkflow.Ack> acks = new ArrayList<>();
        private AiDifyWorkflow workflow;
        private boolean offline;
        private boolean disorder;
        private boolean probeReplay;
        private boolean mismatchFirst;
        private String status = "succeeded";
        private String receipt;

        FakeDify(ObjectMapper mapper) {
            super(mapper);
            this.mapper = mapper;
        }

        AiDifyWorkflow workflow() {
            if (workflow == null) {
                workflow = new AiDifyWorkflow(null, this, BASE_URL, APP_KEY, TIMEOUT);
            }
            return workflow;
        }

        @SuppressWarnings("unchecked")
        @Override
        JsonNode request(String method, String url, Object body, String header, String value, int timeout)
                throws IOException {
            if (offline) {
                throw new IOException("connect timed out");
            }
            Map<String, Object> inputs = (Map<String, Object>) ((Map<String, Object>) body).get("inputs");
            String route = String.valueOf(inputs.get("route"));
            String ticket = String.valueOf(inputs.get("ticket"));
            List<String> plan = plan(route);
            String current = "";
            for (int i = 0; i < plan.size(); i++) {
                if (mismatchFirst && i == 0) {
                    probes.add(workflow.callback(ticket, AiDifyWorkflow.RANK));
                    return failed();
                }
                String stage = disorder && i == 1 ? AiDifyWorkflow.FINALIZE : plan.get(i);
                AiDifyWorkflow.Ack ack = workflow.callback(ticket, stage);
                if (!ack.accepted()) {
                    throw new IOException("callback rejected at " + stage);
                }
                if (probeReplay && i == 0) {
                    probes.add(workflow.callback(ticket, AiDifyWorkflow.REWRITE));
                }
                acks.add(ack);
                current = ack.body();
                ticket = current;
                stages.add(stage);
            }
            ObjectNode response = mapper.createObjectNode();
            response.put("workflow_run_id", "run_abc123");
            ObjectNode data = response.putObject("data");
            data.put("status", status);
            ObjectNode outputs = data.putObject("outputs");
            outputs.put("receipt", receipt == null ? current : receipt);
            outputs.put("route", route);
            return response;
        }

        private JsonNode failed() {
            ObjectNode response = mapper.createObjectNode();
            response.put("workflow_run_id", "run_abc123");
            ObjectNode data = response.putObject("data");
            data.put("status", "failed");
            data.putObject("outputs");
            return response;
        }
    }
}
