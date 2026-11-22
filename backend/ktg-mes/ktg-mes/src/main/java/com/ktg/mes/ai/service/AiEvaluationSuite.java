package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import lombok.Data;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiEvaluationSuite {
    private final ObjectMapper json;
    private final Path manifest;

    public AiEvaluationSuite(ObjectMapper json, Environment environment) {
        this.json = json;
        String configured = environment.getProperty("mes.ai.evaluation.suite-file", "").trim();
        manifest = configured.isEmpty() ? null : Paths.get(configured);
    }

    public boolean available() {
        return manifest != null && Files.isReadable(manifest) && Files.isRegularFile(manifest);
    }

    public Suite load() {
        if (!available()) throw invalid("当前环境没有配置完整的演示验收集。");
        try {
            if (Files.size(manifest) > 256 * 1024) throw invalid("演示验收清单超过大小限制。");
            try (InputStream input = Files.newInputStream(manifest)) {
                Suite suite = json.readValue(input, Suite.class);
                validate(suite);
                return suite;
            }
        } catch (IOException e) {
            throw invalid("演示验收清单无法读取。");
        }
    }

    private static void validate(Suite suite) {
        if (suite == null || !text(suite.name, 100) || suite.libraries == null || suite.libraries.size() != 3
            || suite.numbers == null || suite.numbers.size() != 20) {
            throw invalid("演示验收集应包含三个部门和 20 道数据题。");
        }
        Set<Long> libraries = new HashSet<>();
        Set<Long> departments = new HashSet<>();
        Set<String> questions = new HashSet<>();
        Set<String> triples = new HashSet<>();
        for (Library library : suite.libraries) {
            if (library == null || library.libraryId <= 0 || library.deptId <= 0
                || !libraries.add(library.libraryId) || !departments.add(library.deptId)
                || library.documents == null || library.documents.size() != 5) {
                throw invalid("每个演示部门需要五份独立的资料。");
            }
            Set<String> titles = new HashSet<>();
            for (Document document : library.documents) {
                if (document == null || !text(document.title, 120) || !titles.add(document.title)
                    || document.cases == null || document.cases.size() != 2
                    || document.triples == null || document.triples.isEmpty()) {
                    throw invalid("每份演示资料需要两道文档题和语义关系标注。");
                }
                for (DocumentQuestion question : document.cases) {
                    if (question == null || !text(question.question, 600) || !text(question.fragment, 400)
                        || !questions.add(question.question)) throw invalid("文档题格式无效或重复。");
                }
                for (Triple triple : document.triples) {
                    if (triple == null || !text(triple.source, 100) || !text(triple.target, 100)
                        || !"USED_IN".equals(triple.kind) || !triples.add(triple.key())) {
                        throw invalid("三元组标注格式无效或重复。");
                    }
                }
            }
        }
        if (triples.size() != 100) throw invalid("演示验收集需要 100 条不同的语义关系。");
        for (NumberQuestion question : suite.numbers) {
            if (question == null || !text(question.question, 600) || !questions.add(question.question)
                || !text(question.code, 80) || NumberKind.of(question.kind) == null) {
                throw invalid("数据题格式无效或重复。");
            }
            if ("inbound".equals(question.kind)
                && (question.from == null || question.to == null || question.from.isAfter(question.to))) {
                throw invalid("入库样例的日期范围无效。");
            }
        }
    }

    private static boolean text(String value, int limit) {
        return value != null && !value.trim().isEmpty() && value.length() <= limit;
    }

    private static ServiceException invalid(String message) {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }

    enum NumberKind {
        STOCK("stock", "stock", "itemCode", "balance", "库存表合计"),
        PRODUCED("produced", "workorders", "workorderCode", "produced", "工单已生产量"),
        INBOUND("inbound", "movements", "itemCode", "incoming", "入库流水合计");

        final String id;
        final String tool;
        final String codeColumn;
        final String valueColumn;
        final String source;

        NumberKind(String id, String tool, String codeColumn, String valueColumn, String source) {
            this.id = id;
            this.tool = tool;
            this.codeColumn = codeColumn;
            this.valueColumn = valueColumn;
            this.source = source;
        }

        static NumberKind of(String value) {
            for (NumberKind kind : values()) if (kind.id.equals(value)) return kind;
            return null;
        }
    }

    @Data
    public static class Suite {
        private String name;
        private List<Library> libraries;
        private List<NumberQuestion> numbers;
    }

    @Data
    public static class Library {
        private long libraryId;
        private long deptId;
        private List<Document> documents;
    }

    @Data
    public static class Document {
        private String title;
        private List<DocumentQuestion> cases;
        private List<Triple> triples;
    }

    @Data
    public static class DocumentQuestion {
        private String question;
        private String fragment;
    }

    @Data
    public static class NumberQuestion {
        private String kind;
        private String code;
        private String question;
        private LocalDate from;
        private LocalDate to;
    }

    @Data
    public static class Triple {
        private String source;
        private String kind;
        private String target;

        String key() {
            return source + "|" + kind + "|" + target;
        }
    }
}
