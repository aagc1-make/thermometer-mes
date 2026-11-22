package com.ktg.mes.ai.service;

import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.AiEvaluationMapper;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ktg.mes.ai.service.KbRagEngines.object;
import com.ktg.mes.ai.service.AiEvaluationSuite.NumberKind;

@Service
public class AiEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(AiEvaluationService.class);
    private static final String BOM_TITLE = "测温枪 BOM 关系演示";
    private static final List<DocumentCase> DOCUMENTS = Arrays.asList(
        new DocumentCase("红外传感器来料检验应记录什么？", "来料检验规范（问答演示）", "记录批次与检验员"),
        new DocumentCase("红外传感器模块 TG-EL-002 用于哪个产品？", BOM_TITLE, "用于测温显示电控预装件")
    );
    private static final List<String> STOCK_CODES = Arrays.asList("TG-EL-002", "TG-SF-001", "TG-ST-001");
    private static final Map<String, Object> BOM_RELATIONS = object(
        "Item:TG-EL-002", "Product:TG-SF-001", "Item:TG-EL-001", "Product:TG-SF-001",
        "Item:TG-EL-003", "Product:TG-SF-001", "Item:TG-ST-004", "Product:TG-SF-001",
        "Item:TG-ST-001", "Product:TG-FG-001", "Item:TG-ST-002", "Product:TG-FG-001"
    );

    private final AiEvaluationMapper baseline;
    private final KbRagService rag;
    private final KbGraphService graphs;
    private final AiBusinessAnswer business;
    private final AiReadTools tools;
    private final AiEvaluationSuite acceptance;
    private final KbDocumentService libraries;

    public AiEvaluationService(AiEvaluationMapper baseline, KbRagService rag, KbGraphService graphs,
                               AiBusinessAnswer business, AiReadTools tools, AiEvaluationSuite acceptance,
                               KbDocumentService libraries) {
        this.baseline = baseline;
        this.rag = rag;
        this.graphs = graphs;
        this.business = business;
        this.tools = tools;
        this.acceptance = acceptance;
        this.libraries = libraries;
    }

    public Map<String, Object> run(KbDocument.Library scope) {
        Report report = new Report();
        for (DocumentCase sample : DOCUMENTS) assessDocument(scope, sample, report.recall, report.citations);
        for (String code : STOCK_CODES) assessNumber(NumberKind.STOCK, code, "查询 " + code + " 当前库存", null, null, report.numbers);
        Set<String> expected = BOM_RELATIONS.entrySet().stream()
            .map(pair -> pair.getKey() + "|USED_IN|" + pair.getValue()).collect(Collectors.toSet());
        assessTriples(scope, Collections.singletonMap(BOM_TITLE, expected), report.triples);
        return report.result(scope.getName(), "测温枪演示样例");
    }

    public boolean suiteAvailable() {
        return acceptance.available();
    }

    public Map<String, Object> runSuite() {
        AiEvaluationSuite.Suite suite = acceptance.load();
        Report report = new Report();
        List<KbDocument.Library> scopes = new ArrayList<>();
        for (AiEvaluationSuite.Library library : suite.getLibraries()) {
            KbDocument.Library scope = libraries.readScope(library.getLibraryId());
            if (scope.getDeptId() != library.getDeptId()) {
                throw new ServiceException("演示验收清单的部门库与当前环境不符。", HttpStatus.BAD_REQUEST);
            }
            scopes.add(scope);
        }
        for (int i = 0; i < scopes.size(); i++) {
            KbDocument.Library scope = scopes.get(i);
            Map<String, Set<String>> expected = new LinkedHashMap<>();
            for (AiEvaluationSuite.Document document : suite.getLibraries().get(i).getDocuments()) {
                for (AiEvaluationSuite.DocumentQuestion question : document.getCases()) {
                    assessDocument(scope, new DocumentCase(question.getQuestion(), document.getTitle(),
                        question.getFragment()), report.recall, report.citations);
                }
                expected.put(document.getTitle(), document.getTriples().stream()
                    .map(AiEvaluationSuite.Triple::key).collect(Collectors.toSet()));
            }
            assessTriples(scope, expected, report.triples);
        }
        for (AiEvaluationSuite.NumberQuestion question : suite.getNumbers()) {
            assessNumber(NumberKind.of(question.getKind()), question.getCode(), question.getQuestion(),
                question.getFrom(), question.getTo(), report.numbers);
        }
        return report.result(scopes.stream().map(KbDocument.Library::getName).collect(Collectors.joining("、")), suite.getName());
    }

    private void assessDocument(KbDocument.Library scope, DocumentCase sample, Metric recall, Metric coverage) {
        String evidence = sample.title + "\n" + sample.fragment;
        try {
            Set<Long> gold = new HashSet<>(baseline.goldChunks(scope, sample.title, sample.fragment));
            if (gold.isEmpty()) {
                recall.skip(sample.question, evidence, "当前部门库缺少对应的标注文档分块。");
                coverage.skip(sample.question, evidence, "当前部门库缺少对应的标注文档分块。");
                return;
            }
            KbRagService.ExtractiveSample actual = rag.sample(scope, sample.question);
            int matched = (int) actual.ranked.stream().filter(ref -> gold.contains(ref.getChunkId())).count();
            String ranked = actual.ranked.stream().map(ref -> ref.getTitle() + " · " + ref.getSectionLabel())
                .collect(Collectors.joining("\n"));
            recall.record(sample.question, evidence, ranked.isEmpty() ? "没有检索到候选分块。" : ranked, matched, gold.size());

            if (actual.citations.isEmpty()) {
                coverage.skip(sample.question, "每条证据摘录都带有对应的文档引用。", "本题没有可核对的证据摘录。");
                return;
            }
            int covered = 0;
            for (int i = 0; i < actual.citations.size(); i++) {
                Map<String, Object> reference = actual.citations.get(i);
                KbCitation rankedChunk = actual.ranked.get(i);
                int number = i + 1;
                if (rankedChunk.getVersionId().equals(reference.get("versionId"))
                    && Integer.valueOf(rankedChunk.getChunkIndex()).equals(reference.get("chunkIndex"))
                    && actual.answer.contains(number + ". " + reference.get("excerpt") + " [" + number + "]")) {
                    covered++;
                }
            }
            coverage.record(sample.question, "每条证据摘录都带有对应的文档引用。", actual.answer, covered, actual.citations.size());
        } catch (IOException | RuntimeException e) {
            log.warn("文档样例评测失败，知识库 {}", scope.getLibraryId(), e);
            recall.skip(sample.question, evidence, "检索评测暂不可用。");
            coverage.skip(sample.question, evidence, "引用评测暂不可用。");
        }
    }

    private void assessNumber(NumberKind kind, String code, String question, LocalDate from, LocalDate to,
                              Metric metric) {
        try {
            BigDecimal expected = expectedNumber(kind, code, from, to);
            if (expected == null) {
                metric.skip(question, kind.source, "数据库缺少该样例的基准记录。");
                return;
            }
            AiBusinessRules.Call rule = business.findRule(question);
            String expectedText = kind.source + " " + expected.stripTrailingZeros().toPlainString();
            if (rule == null || !kind.tool.equals(rule.code)) {
                metric.record(question, expectedText, "没有匹配到预期的查询规则。", 0, 1);
                return;
            }
            AiReadTools.Result answer = tools.execute(rule.code, rule.args);
            List<Map<String, Object>> rows = answer.getRows();
            if (rows.size() != 1 || !code.equals(rows.get(0).get(kind.codeColumn))) {
                metric.record(question, expectedText, "未返回该编码的唯一结果。", 0, 1);
                return;
            }
            BigDecimal actual = new BigDecimal(rows.get(0).get(kind.valueColumn).toString());
            metric.record(question, expectedText, "助手返回 " + actual.stripTrailingZeros().toPlainString(),
                expected.compareTo(actual) == 0 ? 1 : 0, 1);
        } catch (RuntimeException e) {
            log.warn("数据样例评测失败，编码 {}", code, e);
            metric.skip(question, kind.source, "数据样例暂不可用。");
        }
    }

    private BigDecimal expectedNumber(NumberKind kind, String code, LocalDate from, LocalDate to) {
        switch (kind) {
            case PRODUCED: return baseline.produced(code);
            case INBOUND: return baseline.inbound(code, from, to);
            default: return baseline.stock(code);
        }
    }

    private void assessTriples(KbDocument.Library scope, Map<String, Set<String>> documents, Metric metric) {
        Map<String, Set<Long>> expected = new LinkedHashMap<>();
        Set<Long> chunks = new HashSet<>();
        try {
            for (Map.Entry<String, Set<String>> document : documents.entrySet()) {
                Set<Long> ids = new HashSet<>(baseline.goldChunks(scope, document.getKey(), ""));
                if (ids.isEmpty()) {
                    metric.skip(document.getKey(), document.getValue().size() + " 条标注关系", "当前部门库缺少该标注文档。");
                    continue;
                }
                chunks.addAll(ids);
                for (String relation : document.getValue()) {
                    expected.computeIfAbsent(relation, key -> new HashSet<>()).addAll(ids);
                }
            }
            if (chunks.isEmpty()) return;
            KbGraphService.Graph graph = graphs.sample(scope, new ArrayList<>(chunks));
            Set<String> observed = new HashSet<>();
            for (KbGraphService.Edge edge : graph.getEdges()) {
                if ("MENTIONS".equals(edge.getKind())) continue;
                String key = edge.getSource() + "|" + edge.getKind() + "|" + edge.getTarget();
                boolean matches = expected.containsKey(key) && !Collections.disjoint(expected.get(key), edge.getEvidence());
                if (matches) observed.add(key);
                String relation = key.replace("|", " → ");
                metric.record(relation, matches ? relation : "标注中未找到来自该分块的对应关系。",
                    edge.getFromName() + " → " + edge.getKind() + " → " + edge.getToName(), matches ? 1 : 0, 1);
            }
            for (String key : expected.keySet()) {
                if (observed.contains(key)) continue;
                String relation = key.replace("|", " → ");
                metric.skip(relation + "（标注）", relation, graph.isLimited()
                    ? "该标注未出现在图谱返回的有限抽样中。" : "图谱未抽到来自标注文档的关系。");
            }
        } catch (IOException | RuntimeException e) {
            log.warn("图谱样例评测失败，知识库 {}", scope.getLibraryId(), e);
            metric.skip(scope.getName(), "标注文档中的语义关系", "图谱服务暂不可用。");
        }
    }

    private static final class Report {
        private final long start = System.currentTimeMillis();
        private final Metric recall = new Metric("recall", "Recall@5", "前五条候选命中的标注分块数 ÷ 已发布的标注分块数。", "分块");
        private final Metric numbers = new Metric("numbers", "数值正确率", "规则问答数值与独立 SQL 结果相符的题数 ÷ 已评测题数。", "题");
        private final Metric citations = new Metric("citations", "引用覆盖率", "引用编号与原文证据一一对应的摘录数 ÷ 仅检索回答的证据摘录数。", "摘录");
        private final Metric triples = new Metric("triples", "三元组抽检准确率", "与标注相符的三元组数 ÷ 本轮实际抽到的语义三元组数。", "条");

        Map<String, Object> result(String scope, String sample) {
            return object("libraryName", scope, "sampleName", sample, "finishedAt", Instant.now().toString(),
                "elapsedMs", System.currentTimeMillis() - start, "metrics", Arrays.asList(recall, numbers, citations, triples));
        }
    }

    @Getter
    public static final class Metric {
        private final String key;
        private final String label;
        private final String definition;
        private final String unit;
        private final List<Map<String, Object>> cases = new ArrayList<>();
        private int matched;
        private int total;
        private int skipped;

        Metric(String key, String label, String definition, String unit) {
            this.key = key;
            this.label = label;
            this.definition = definition;
            this.unit = unit;
        }

        void record(String name, String expected, String actual, int passed, int count) {
            matched += passed;
            total += count;
            cases.add(object("name", name, "expected", expected, "actual", actual,
                "status", passed == count ? "passed" : "failed", "matched", passed, "total", count));
        }

        void skip(String name, String expected, String reason) {
            skipped++;
            cases.add(object("name", name, "expected", expected, "actual", reason, "status", "skipped"));
        }
    }

    private static final class DocumentCase {
        final String question;
        final String title;
        final String fragment;

        DocumentCase(String question, String title, String fragment) {
            this.question = question;
            this.title = title;
            this.fragment = fragment;
        }
    }
}
