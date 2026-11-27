package com.ktg.mes.ai.service;

import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.KbRetrievalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ktg.mes.ai.service.KbRagEngines.object;

/**
 * 把「这一段装配」挂到授权知识库里的原文上。
 *
 * <p>V1 只接受两类强依据：① 原文明说该子件装入该父件的肯定句；
 * ② 有唯一父件上下文的直属 BOM 表格行（标题点名父件，或同文档内只出现一个整机成品）。
 * 共同提及、工序流转和「最终装配于」只作为检索线索，不能把两件物料直接连起来。</p>
 */
@Service
public class ImpactEvidenceService {
    private static final Logger log = LoggerFactory.getLogger(ImpactEvidenceService.class);

    private static final int MAX_CHUNKS = 12;
    private static final int WINDOW = 2;
    private static final int MAX_SOURCES = 3;
    private static final int MAX_EXCERPT = 320;
    private static final int MAX_FINISHED = 200;
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s*(.+?)\\s*#*\\s*$");
    private static final Pattern SECTION = Pattern.compile("^(\\d+(?:\\.\\d+)*)[\\s\\u3000]*(.+)$");
    private static final Pattern QUANTITY = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*([\\p{IsHan}A-Za-z]{0,3})\\s*/\\s*([\\p{IsHan}A-Za-z]{1,4})");
    private static final Pattern COMPOSITION = Pattern.compile(
        ".*(?:直属|直接|整机|成品).*(?:组成|物料).*|.*(?:组成|物料).*(?:整机|成品).*");
    /** 否定、替代、工序流转和间接表述都不能证明直接装配。 */
    private static final String EXCLUDED = "不使用|不用于|不包含|不含|无需|禁止|替代|除外|未使用|"
        + "最终装配于|工序|流转|不属于|不能|不再";
    private static final String CHILD_FIRST = "用于|应用到|用在|装入|装配到|装到|装在|供给|组成|构成";
    private static final String PARENT_FIRST = "使用|采用|包含|含|需要|由|组成|装有|配备|配有";

    private final KbRetrievalMapper retrieval;

    public ImpactEvidenceService(KbRetrievalMapper retrieval) {
        this.retrieval = retrieval;
    }

    /** 返回该段可引用的原文；没有强依据时返回空数组，文档查询异常由调用方标记 unavailable。 */
    public List<Map<String, Object>> find(KbDocument.Library scope, String childCode, String parentCode,
                                          BigDecimal quantity) {
        if (isEmpty(childCode) || isEmpty(parentCode) || childCode.equalsIgnoreCase(parentCode)) {
            return Collections.emptyList();
        }
        List<KbCitation> hits = retrieval.impactChunks(scope, childCode, MAX_CHUNKS);
        if (hits.isEmpty()) return Collections.emptyList();
        Pattern child = pattern(childCode);
        Pattern parent = pattern(parentCode);
        Map<Long, List<String>> finished = new HashMap<>();
        Map<Long, List<KbCitation>> windows = new LinkedHashMap<>();
        for (KbCitation hit : hits) {
            if (windows.containsKey(hit.getVersionId())) continue;
            List<KbCitation> window = retrieval.impactVersionChunks(scope, hit.getVersionId(),
                hit.getChunkIndex() - WINDOW, hit.getChunkIndex() + WINDOW);
            windows.put(hit.getVersionId(), window.isEmpty() ? Collections.singletonList(hit) : window);
        }
        List<Match> matches = new ArrayList<>();
        for (List<KbCitation> window : windows.values()) {
            try {
                matches.addAll(match(scope, window, child, parent, parentCode, finished));
            } catch (RuntimeException e) {
                log.warn("解析影响路径的文档片段失败，版本 {}", window.get(0).getVersionId(), e);
            }
        }
        matches.sort((left, right) -> left.rank != right.rank ? left.rank - right.rank
            : left.chunk.getVersionId().equals(right.chunk.getVersionId())
                ? Integer.compare(left.chunk.getChunkIndex(), right.chunk.getChunkIndex())
                : Long.compare(left.chunk.getVersionId(), right.chunk.getVersionId()));
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> sources = new ArrayList<>();
        for (Match match : matches) {
            String key = match.chunk.getVersionId() + ":" + match.chunk.getChunkIndex() + ":" + match.excerpt;
            if (!seen.add(key)) continue;
            sources.add(source(scope, match, quantity));
            if (sources.size() >= MAX_SOURCES) break;
        }
        return sources;
    }

    // ------------------------------------------------------------------ 匹配规则

    private List<Match> match(KbDocument.Library scope, List<KbCitation> window, Pattern child, Pattern parent,
                              String parentCode, Map<Long, List<String>> finished) {
        List<Match> matches = new ArrayList<>();
        List<Line> lines = lines(window);
        List<Table> tables = new ArrayList<>();
        Heading heading = null;
        Table table = null;
        for (Line line : lines) {
            String text = line.text.trim();
            Matcher mark = HEADING.matcher(text);
            if (mark.matches()) {
                heading = new Heading(mark.group(2), line.chunk);
                table = null;
                continue;
            }
            if (text.startsWith("|")) {
                if (table == null) {
                    table = new Table(heading, line.chunk);
                    tables.add(table);
                }
                table.rows.add(line);
            } else {
                table = null;
            }
        }
        for (Table block : tables) {
            Match match = tableMatch(scope, block, child, parent, parentCode, finished);
            if (match != null) matches.add(match);
        }
        for (Line line : lines) {
            String text = line.text.trim();
            if (text.isEmpty() || text.startsWith("|") || text.startsWith("#")) continue;
            for (String sentence : text.split("[。；！？;!?]+")) {
                Match match = sentenceMatch(sentence, line.chunk, child, parent);
                if (match != null) matches.add(match);
            }
        }
        return matches;
    }

    /** 表格行：标题点名父件（强），或「整机直属组成」式标题 + 同文档唯一整机（次强）。 */
    private Match tableMatch(KbDocument.Library scope, Table table, Pattern child, Pattern parent,
                             String parentCode, Map<Long, List<String>> finished) {
        if (table.rows.isEmpty() || table.heading == null || table.chunk == null) return null;
        Line target = null;
        for (Line row : table.rows) {
            if (indexOf(row.text, child) >= 0) {
                target = row;
                break;
            }
        }
        if (target == null) return null;
        String heading = table.heading.text;
        String clause = clauseLabel(heading);
        if (indexOf(heading, parent) >= 0) {
            return match(1, target, clause, table.heading);
        }
        if (!COMPOSITION.matcher(heading).matches() || !table.rows.get(0).text.contains("编码")) {
            return null;
        }
        String code = uniqueFinished(scope, table.chunk.getVersionId(), finished);
        return code != null && code.equalsIgnoreCase(parentCode) ? match(3, target, clause, table.heading) : null;
    }

    /** 同文档内只出现一个整机成品时，才接受无编码标题的直属组成表。 */
    private String uniqueFinished(KbDocument.Library scope, long versionId, Map<Long, List<String>> cache) {
        List<String> codes = cache.get(versionId);
        if (codes == null) {
            codes = retrieval.impactFinishedCodes(scope, versionId, BomImpactService.FINISHED_TYPE, MAX_FINISHED);
            cache.put(versionId, codes);
        }
        return codes.size() == 1 ? codes.get(0) : null;
    }

    /** 整句：两个编码同时出现，方向词在两者之间，排除否定、替代、工序与间接说法。 */
    private Match sentenceMatch(String sentence, KbCitation chunk, Pattern child, Pattern parent) {
        String text = sentence.trim();
        if (text.length() < 4) return null;
        int[] at = hit(text, child);
        int[] pat = hit(text, parent);
        if (at == null || pat == null || at[0] == pat[0]) return null;
        boolean childFirst = at[0] < pat[0];
        String between = childFirst ? cut(text, at[1], pat[0]) : cut(text, pat[1], at[0]);
        if (between.length() > 40 || between.matches(".*(?:" + EXCLUDED + ").*")) return null;
        String verbs = childFirst ? CHILD_FIRST : PARENT_FIRST;
        if (!between.matches(".*(?:" + verbs + ").*")) return null;
        return new Match(2, chunk, trim(text), null, null);
    }

    private static Match match(int rank, Line row, String clause, Heading heading) {
        KbCitation context = heading != null && heading.chunk != null
            && !heading.chunk.getChunkId().equals(row.chunk.getChunkId()) ? heading.chunk : null;
        return new Match(rank, row.chunk,
            trim(row.text.replaceAll("^\\|", "").replaceAll("\\|$", "")), clause, context);
    }

    // ------------------------------------------------------------------ 片段拼装

    /** 把同版本的相邻片段拼回文档结构；被分块截断的表格行要接回去。 */
    private static List<Line> lines(List<KbCitation> chunks) {
        StringBuilder text = new StringBuilder();
        List<KbCitation> marks = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        for (KbCitation chunk : chunks) {
            String content = chunk.getContent() == null ? "" : chunk.getContent();
            if (text.length() > 0) {
                String last = lastLine(text).trim();
                boolean join = last.startsWith("|") && !last.endsWith("|");
                if (!join) text.append('\n');
            }
            starts.add(text.length());
            marks.add(chunk);
            text.append(content);
        }
        List<Line> lines = new ArrayList<>();
        int from = 0;
        while (from <= text.length()) {
            int end = text.indexOf("\n", from);
            if (end < 0) end = text.length();
            lines.add(new Line(text.substring(from, end), chunkAt(starts, marks, from)));
            from = end + 1;
        }
        return lines;
    }

    private static String lastLine(StringBuilder text) {
        int at = text.lastIndexOf("\n");
        return text.substring(at + 1);
    }

    private static KbCitation chunkAt(List<Integer> starts, List<KbCitation> marks, int offset) {
        KbCitation found = marks.isEmpty() ? null : marks.get(0);
        for (int i = 0; i < starts.size(); i++) {
            if (starts.get(i) <= offset) found = marks.get(i);
            else break;
        }
        return found;
    }

    // ------------------------------------------------------------------ 输出

    private static Map<String, Object> source(KbDocument.Library scope, Match match, BigDecimal quantity) {
        Map<String, Object> source = object("libraryId", String.valueOf(scope.getLibraryId()),
            "versionId", String.valueOf(match.chunk.getVersionId()), "version", match.chunk.getVersion(),
            "title", match.chunk.getTitle(), "filename", match.chunk.getFilename(),
            "chunkIndex", match.chunk.getChunkIndex(), "sectionLabel", match.chunk.getSectionLabel(),
            "clauseLabel", match.clause, "excerpt", match.excerpt);
        Matcher quoted = QUANTITY.matcher(match.excerpt);
        if (quoted.find()) {
            String documentQuantity = (quoted.group(1) + " " + quoted.group(2) + "/" + quoted.group(3))
                .replaceAll("\\s+", " ").trim();
            source.put("documentQuantity", documentQuantity);
            try {
                if (quantity != null) {
                    source.put("quantityMismatch",
                        new BigDecimal(quoted.group(1)).compareTo(quantity) != 0);
                }
            } catch (NumberFormatException ignored) {
                // 原文用量写法无法解析时只展示原文，不做数值对比
            }
        }
        if (match.context != null) {
            source.put("contextLocations", Collections.singletonList(object(
                "libraryId", String.valueOf(scope.getLibraryId()),
                "versionId", String.valueOf(match.context.getVersionId()),
                "chunkIndex", match.context.getChunkIndex(),
                "sectionLabel", match.context.getSectionLabel())));
        }
        return source;
    }

    static String clauseLabel(String heading) {
        String text = heading == null ? "" : heading.replace('\u3000', ' ').replaceAll("\\s+", " ").trim();
        Matcher numbered = SECTION.matcher(text);
        return numbered.matches() ? "第 " + numbered.group(1) + " 节 " + numbered.group(2).trim() : text;
    }

    private static Pattern pattern(String code) {
        return Pattern.compile("(?<![A-Za-z0-9_.-])" + Pattern.quote(code) + "(?![A-Za-z0-9_.-])",
            Pattern.CASE_INSENSITIVE);
    }

    private static int[] hit(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? new int[]{matcher.start(), matcher.end()} : null;
    }

    private static int indexOf(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private static String cut(String text, int from, int to) {
        return from < 0 || to < 0 || from > to || to > text.length() ? "" : text.substring(from, to);
    }

    private static String trim(String text) {
        String value = text == null ? "" : text.replace('\u3000', ' ').replaceAll("\\s+", " ").trim();
        return value.length() <= MAX_EXCERPT ? value : value.substring(0, MAX_EXCERPT) + "…";
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class Line {
        private final String text;
        private final KbCitation chunk;

        Line(String text, KbCitation chunk) {
            this.text = text;
            this.chunk = chunk;
        }
    }

    private static class Heading {
        private final String text;
        private final KbCitation chunk;

        Heading(String text, KbCitation chunk) {
            this.text = text;
            this.chunk = chunk;
        }
    }

    private static class Table {
        private final Heading heading;
        private final KbCitation chunk;
        private final List<Line> rows = new ArrayList<>();

        Table(Heading heading, KbCitation chunk) {
            this.heading = heading;
            this.chunk = chunk;
        }
    }

    private static class Match {
        private final int rank;
        private final KbCitation chunk;
        private final String excerpt;
        private final String clause;
        private final KbCitation context;

        Match(int rank, KbCitation chunk, String excerpt, String clause, KbCitation context) {
            this.rank = rank;
            this.chunk = chunk;
            this.excerpt = excerpt;
            this.clause = clause;
            this.context = context;
        }
    }
}
