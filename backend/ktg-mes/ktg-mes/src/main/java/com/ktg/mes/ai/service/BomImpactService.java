package com.ktg.mes.ai.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.mapper.AiReadToolMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ktg.mes.ai.service.KbRagEngines.object;

/**
 * 物料影响路径：按 MES 物料清单反向（子件 → 父件）查出有序装配路径。
 *
 * <p>装配事实全部来自 {@code md_product_bom} 的启用行，每段保留原始行号与直接用量；
 * 当前可读知识库只用于给某一段附上原文佐证（见 {@link ImpactEvidenceService}）。
 * 原有的向下 BOM 展开（{@code AiReadTools} 的 {@code bom} 工具）保持不变。</p>
 */
@Service
public class BomImpactService {
    private static final Logger log = LoggerFactory.getLogger(BomImpactService.class);

    /** 演示库用物料类型编码标识整机成品；成品判断集中在这一处，普通 Product 不一律当作整机。 */
    static final String FINISHED_TYPE = "TG_FG";
    static final int MAX_DEPTH = 6;
    static final int MAX_PATHS = 20;
    static final int MAX_EXPANSIONS = 200;
    private static final int MAX_LINKS = 200;
    private static final int MAX_ROWS_PER_QUERY = 400;
    private static final int MAX_VIA = 6;
    private static final int MAX_CANDIDATES = 6;
    private static final int MAX_CITATIONS = 4;
    private static final int UNAVAILABLE = 503;
    private static final String USED_IN = "BOM_USED_IN";
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{2,63}");
    private static final Pattern CODE_TOKEN = Pattern.compile("[A-Za-z][A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)+");
    /** 向上查装配的说法；向下展开、库存、工序与纯文档提问不会命中。 */
    private static final Pattern UPWARD = Pattern.compile(
        "用在(?:哪|什么|哪个|哪些|何处|哪里)|装(?:到|在|入)(?:哪|什么|哪个|哪些|何处|哪里)"
            + "|哪(?:个|款|些|一种)(?:产品|整机|成品)|(?:上级|上层)(?:装配|物料|产品|结构)"
            + "|影响路径|向上(?:查|追溯)|最终(?:用在|用于|装(?:到|入)|装配)|往(?:上|上查)"
            + "|(?:产品|整机|成品)(?:中|里)(?:的)?(?:哪个|哪款)");

    private final AiReadToolMapper mapper;
    private final ImpactEvidenceService evidence;
    private final PermissionService permissions;

    public BomImpactService(AiReadToolMapper mapper, ImpactEvidenceService evidence,
                            PermissionService permissions) {
        this.mapper = mapper;
        this.evidence = evidence;
        this.permissions = permissions;
    }

    // ------------------------------------------------------------------ 路径查询

    /** 按完整编码查出起点、边与有序路径。装配事实只来自物料清单，与部门库无关。 */
    public Impact impact(String itemCode) {
        String code = itemCode(itemCode);
        requireBomRead();
        Map<String, Object> item = mapper.impactItem(code);
        if (item == null) {
            throw new ServiceException("没有找到编码为 " + code + " 的物料或产品。", HttpStatus.NOT_FOUND);
        }
        long originId = number(item.get("itemId"));
        Node origin = nodeOfItem(item);
        Impact result = new Impact(origin.getId());
        Map<Long, Node> nodes = new LinkedHashMap<>();
        nodes.put(originId, origin);
        Map<String, Edge> edges = new LinkedHashMap<>();
        Map<Long, List<Link>> adjacency = new LinkedHashMap<>();
        Set<String> reasons = new LinkedHashSet<>();

        // 逐层批量反查：每层只用一次 SQL，只保留启用关系
        Set<Long> frontier = new LinkedHashSet<>(Collections.singletonList(originId));
        int expansions = 0;
        int depth = 0;
        while (!frontier.isEmpty()) {
            if (depth >= MAX_DEPTH) {
                // 多探一层确认是否真的还有上级，避免把“已查完”说成“被截断”
                if (parents(frontier, 1).size() > 0) reasons.add("depth_limit");
                break;
            }
            int budget = MAX_EXPANSIONS - expansions;
            if (budget <= 0) {
                if (parents(frontier, 1).size() > 0) reasons.add("expansion_limit");
                break;
            }
            List<Map<String, Object>> rows = parents(frontier, Math.min(budget + 1, MAX_ROWS_PER_QUERY));
            if (rows.size() > budget) {
                reasons.add("expansion_limit");
                rows = rows.subList(0, budget);
            }
            Set<Long> next = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                long childId = number(row.get("childId"));
                long parentId = number(row.get("parentId"));
                Node child = nodes.get(childId);
                if (child == null || parentId == 0 || childId == parentId) continue;
                String edgeId = "bom:" + row.get("bomId");
                Edge edge = edges.get(edgeId);
                if (edge == null) {
                    if (edges.size() >= MAX_LINKS) {
                        reasons.add("candidate_limit");
                        break;
                    }
                    edge = new Edge(edgeId, text(row.get("bomId")), USED_IN, child.getId(),
                        nodeIdOfProduct(row, "parent"), decimal(row.get("quantity")),
                        text(row.get("childUnit")), text(row.get("parentUnit")));
                    edges.put(edgeId, edge);
                }
                Node parent = nodeOfParent(row);
                Node known = nodes.get(parentId);
                if (known == null) {
                    nodes.put(parentId, parent);
                    next.add(parentId);
                }
                adjacency.computeIfAbsent(childId, key -> new ArrayList<>())
                    .add(new Link(parentId, edge, parent.isFinished()));
                expansions++;
            }
            frontier = next;
            depth++;
        }

        result.getNodes().addAll(nodes.values());
        result.getEdges().addAll(edges.values());
        walk(originId, new ArrayList<>(), new LinkedHashSet<>(Collections.singletonList(originId)), 0,
            adjacency, nodes, result);
        sort(result, nodes);
        result.finish(reasons);
        return result;
    }

    private List<Map<String, Object>> parents(Set<Long> frontier, int limit) {
        try {
            return mapper.impactParents(new ArrayList<>(frontier), Math.max(1, limit));
        } catch (RuntimeException e) {
            log.warn("反向查询物料清单失败，本层 {} 个子件", frontier.size(), e);
            throw new ServiceException("影响路径暂不可读取，请稍后重试。", UNAVAILABLE);
        }
    }

    /** 深度优先：遇到整机、无上级、深度上限或回环时结束当前路径。 */
    private boolean walk(long current, List<String> edgeIds, Set<Long> visited, int depth,
                         Map<Long, List<Link>> adjacency, Map<Long, Node> nodes, Impact result) {
        if (depth >= MAX_DEPTH) {
            return addPath(result, edgeIds, nodes.get(current), "depth_limit");
        }
        List<Link> links = adjacency.get(current);
        if (links == null || links.isEmpty()) {
            return depth == 0 || addPath(result, edgeIds, nodes.get(current), "no_parent");
        }
        for (Link link : links) {
            if (visited.contains(link.parentId)) {
                // 回环只停止这一支，已确认的非循环段仍然保留
                if (!addPath(result, edgeIds, nodes.get(current), "cycle")) return false;
                continue;
            }
            edgeIds.add(link.edge.getId());
            visited.add(link.parentId);
            boolean carryOn = link.finished
                ? addPath(result, edgeIds, nodes.get(link.parentId), "finished_product")
                : walk(link.parentId, edgeIds, visited, depth + 1, adjacency, nodes, result);
            visited.remove(link.parentId);
            edgeIds.remove(edgeIds.size() - 1);
            if (!carryOn) return false;
        }
        return true;
    }

    private static boolean addPath(Impact result, List<String> edgeIds, Node terminal, String reason) {
        if (result.getPaths().size() >= MAX_PATHS) {
            result.limited = true;
            if (!result.getLimitReasons().contains("result_limit")) result.getLimitReasons().add("result_limit");
            return false;
        }
        result.getPaths().add(new Path(new ArrayList<>(edgeIds), terminal == null ? "" : terminal.getId(), reason));
        return true;
    }

    /** 已到达整机的路径优先，其次按长度、终点编码与首个行号稳定排序。 */
    private static void sort(Impact result, Map<Long, Node> nodes) {
        Map<String, String> codes = nodes.values().stream()
            .collect(Collectors.toMap(Node::getId, Node::getCode, (left, right) -> left));
        Comparator<Path> order = Comparator
            .comparing((Path path) -> !"finished_product".equals(path.getStopReason()))
            .thenComparing(path -> path.getEdgeIds().size())
            .thenComparing(path -> codes.getOrDefault(path.getTerminalId(), path.getTerminalId()))
            .thenComparing(path -> path.getEdgeIds().isEmpty() ? "" : path.getEdgeIds().get(0));
        result.getPaths().sort(order);
    }

    // ------------------------------------------------------------------ 单段详情

    /** 点击某段时重新读取启用的 BOM 行、逐段校验链条，并附上授权文档佐证。 */
    public EdgeDetail edge(KbDocument.Library scope, long bomId, String itemCode, String via, String parentCode) {
        String code = itemCode(itemCode);
        String parent = itemCode(parentCode);
        List<Long> chain = via(via);
        requireBomRead();
        requireBomRow();
        Map<String, Object> origin = mapper.impactItem(code);
        if (origin == null) {
            throw new ServiceException("没有找到编码为 " + code + " 的物料或产品。", HttpStatus.NOT_FOUND);
        }
        List<Map<String, Object>> rows;
        try {
            rows = mapper.impactPathRows(chain);
        } catch (RuntimeException e) {
            log.warn("读取 BOM 行失败，行号 {}", chain, e);
            throw new ServiceException("影响路径暂不可读取，请稍后重试。", UNAVAILABLE);
        }
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byId.put(text(row.get("bomId")), row);
        }
        long current = number(origin.get("itemId"));
        Map<String, Object> last = null;
        for (Long rowId : chain) {
            Map<String, Object> row = byId.get(String.valueOf(rowId));
            if (row == null || !"Y".equalsIgnoreCase(text(row.get("enableFlag")))
                || number(row.get("childId")) != current) {
                throw changed();
            }
            current = number(row.get("parentId"));
            last = row;
        }
        if (last == null || number(last.get("bomId")) != bomId
            || !parent.equalsIgnoreCase(text(last.get("parentCode")))) {
            throw changed();
        }

        String childCode = text(last.get("childCode"));
        String parentCodeActual = text(last.get("parentCode"));
        String childName = text(last.get("childName"));
        String parentName = text(last.get("parentName"));
        BigDecimal quantity = decimal(last.get("quantity"));
        Map<String, Object> bomRow = object("bomId", String.valueOf(bomId), "childCode", childCode,
            "childName", childName, "parentCode", parentCodeActual, "parentName", parentName,
            "quantity", quantity, "childUnit", text(last.get("childUnit")),
            "parentUnit", text(last.get("parentUnit")));
        String status = "ready";
        List<Map<String, Object>> sources = Collections.emptyList();
        try {
            sources = evidence.find(scope, childCode, parentCodeActual, quantity);
        } catch (RuntimeException e) {
            log.warn("读取文档佐证失败，BOM 行 {}", bomId, e);
            status = "unavailable";
        }
        return new EdgeDetail(bomRow, status, sources);
    }

    // ------------------------------------------------------------------ 问答意图与上下文

    /** 图谱问答里识别「向上查装配」意图；返回 null 表示继续走原有分支。 */
    public Intent intent(String question) {
        String text = question == null ? "" : question.trim();
        if (text.isEmpty() || !UPWARD.matcher(text).find()) {
            return null;
        }
        List<String> tokens = new ArrayList<>();
        Matcher matcher = CODE_TOKEN.matcher(text);
        while (matcher.find() && tokens.size() < MAX_CANDIDATES) {
            String token = matcher.group();
            if (token.length() >= 4 && token.matches(".*\\d.*") && !tokens.contains(token)) {
                tokens.add(token);
            }
        }
        if (!tokens.isEmpty()) {
            List<Map<String, Object>> items = mapper.impactItemsByCodes(tokens);
            if (items.size() == 1) {
                return new Intent(text(items.get(0).get("itemCode")), Collections.emptyList(), null);
            }
            if (items.size() > 1) {
                return new Intent(null, codes(items), "问题里出现了多个已存在的物料编码："
                    + String.join("、", codes(items)) + "。请只保留要追溯的那一个，我再按 BOM 向上查装配。");
            }
            return new Intent(null, tokens, "没有找到编码为 " + String.join("、", tokens)
                + " 的物料或产品。请确认完整编码，例如 TG-EL-002。");
        }
        List<Map<String, Object>> named = mapper.impactItemsByName(text, MAX_CANDIDATES);
        if (named.size() == 1) {
            return new Intent(text(named.get(0).get("itemCode")), Collections.emptyList(), null);
        }
        if (named.size() > 1) {
            return new Intent(null, codes(named), "匹配到多个物料：" + String.join("、", codes(named))
                + "。请补充要追溯的完整编码。");
        }
        return new Intent(null, Collections.emptyList(),
            "请提供要追溯的物料完整编码（例如 TG-EL-002），我再按 BOM 向上查它被哪些产品使用。");
    }

    /**
     * 问答用的上下文：先取有序关系与 BOM 行，再为需要回答的段取授权条款，
     * 形成「第几段、谁装入谁、直接用量、BOM 行号、文档引用」的结构化文本。
     */
    public Answer answerFor(KbDocument.Library scope, String itemCode) {
        Impact result = impact(itemCode);
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (Node node : result.getNodes()) {
            nodes.putIfAbsent(node.getId(), node);
        }
        Map<String, Edge> edges = new LinkedHashMap<>();
        for (Edge edge : result.getEdges()) {
            edges.putIfAbsent(edge.getId(), edge);
        }
        List<Map<String, Object>> citations = new ArrayList<>();
        StringBuilder facts = new StringBuilder();
        Node origin = nodes.get(result.getOriginId());
        facts.append("起点：").append(label(origin)).append("（").append(text(origin == null ? null : origin.getCategoryName()))
            .append("）\n");
        List<Segment> segments = result.getPaths().isEmpty() ? Collections.emptyList()
            : segments(result.getPaths().get(0), nodes, edges);
        List<String> marks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            String mark = "";
            try {
                List<Map<String, Object>> sources = evidence.find(scope, segment.getChild().getCode(),
                    segment.getParent().getCode(), segment.getEdge().getQuantity());
                for (Map<String, Object> source : sources) {
                    if (citations.size() >= MAX_CITATIONS) break;
                    citations.add(citation(scope, source));
                    if (mark.isEmpty()) mark = "[" + citations.size() + "]";
                }
            } catch (RuntimeException e) {
                log.warn("问答读取文档佐证失败，BOM 行 {}", segment.getEdge().getBomId(), e);
            }
            marks.add(mark);
            facts.append("第 ").append(i + 1).append(" 段：").append(label(segment.getChild()))
                .append(" 装入 ").append(label(segment.getParent()))
                .append("，每").append(text(segment.getEdge().getParentUnit()))
                .append("用 ").append(quantity(segment.getEdge().getQuantity())).append(' ')
                .append(text(segment.getEdge().getChildUnit()))
                .append("，BOM 行 ").append(segment.getEdge().getBomId()).append(mark).append('\n');
        }
        if (result.getPaths().isEmpty()) {
            facts.append("当前没有启用的上级装配记录。\n");
        } else {
            Path path = result.getPaths().get(0);
            facts.append("终点：").append(label(nodes.get(path.getTerminalId())))
                .append("（").append(reasonText(path.getStopReason())).append("）\n");
            if (result.getPaths().size() > 1) {
                facts.append("另有 ").append(result.getPaths().size() - 1).append(" 条已确认路径：")
                    .append(result.getPaths().subList(1, result.getPaths().size()).stream()
                        .map(other -> label(nodes.get(other.getTerminalId())))
                        .collect(Collectors.joining("、")))
                    .append('\n');
            }
        }
        if (result.isLimited()) {
            facts.append("查询达到上限（").append(String.join("、", result.getLimitReasons()))
                .append("），只列出已确认的范围，不表示已找全。\n");
        }
        return new Answer(itemCode, result, citations, facts.toString(),
            summary(result, nodes, segments, marks));
    }

    /** 没有模型时按同一批记录生成简短答案。 */
    static String summary(Impact result, Map<String, Node> nodes, List<Segment> segments, List<String> marks) {
        Node origin = nodes.get(result.getOriginId());
        if (result.getPaths().isEmpty()) {
            return "MES 物料清单里没有 " + label(origin) + " 的启用上级装配记录，当前追溯到此，"
                + "不能据此判断它装入了哪个产品。";
        }
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            answer.append(i == 0 ? "" : "接着，").append(label(segment.getChild()))
                .append("装入 ").append(label(segment.getParent()))
                .append("，每").append(text(segment.getEdge().getParentUnit()))
                .append("用 ").append(quantity(segment.getEdge().getQuantity())).append(' ')
                .append(text(segment.getEdge().getChildUnit()))
                .append("（BOM #").append(segment.getEdge().getBomId()).append("）")
                .append(i < marks.size() ? marks.get(i) : "").append("。");
        }
        Path path = result.getPaths().get(0);
        Node terminal = nodes.get(path.getTerminalId());
        answer.append("因此，").append(named(origin)).append("经 ").append(segments.size())
            .append(" 段装配用在 ").append(named(terminal)).append("。");
        if (!"finished_product".equals(path.getStopReason())) {
            answer.append(reasonText(path.getStopReason())).append('。');
        }
        if (result.getPaths().size() > 1) {
            String more = result.getPaths().subList(1, result.getPaths().size()).stream()
                .map(other -> label(nodes.get(other.getTerminalId())))
                .filter(label -> !label.isEmpty())
                .collect(Collectors.joining("、"));
            answer.append("另有 ").append(result.getPaths().size() - 1).append(" 条已确认路径")
                .append(more.isEmpty() ? "。" : "，分别到达 " + more + "。");
        }
        if (result.isLimited()) {
            answer.append("本次查询达到上限，只列出已确认的范围，不表示已找全。");
        }
        return answer.toString();
    }

    // ------------------------------------------------------------------ 组装与工具方法

    /** 影响路径响应体：起点、节点、边、有序路径与上限说明。 */
    public static Map<String, Object> payload(Impact impact) {
        return object("originId", impact.getOriginId(), "nodes", impact.getNodes(), "edges", impact.getEdges(),
            "paths", impact.getPaths(), "limited", impact.isLimited(),
            "limitReasons", impact.getLimitReasons());
    }

    static Map<String, Object> citation(KbDocument.Library scope, Map<String, Object> source) {
        return object("libraryId", String.valueOf(scope.getLibraryId()), "versionId", source.get("versionId"),
            "version", source.get("version"), "title", source.get("title"), "filename", source.get("filename"),
            "chunkIndex", source.get("chunkIndex"), "sectionLabel", source.get("sectionLabel"),
            "clauseLabel", source.get("clauseLabel"), "excerpt", source.get("excerpt"));
    }

    static List<Segment> segments(Path path, Map<String, Node> nodes, Map<String, Edge> edges) {
        List<Segment> segments = new ArrayList<>();
        for (String edgeId : path.getEdgeIds()) {
            Edge edge = edges.get(edgeId);
            if (edge == null) continue;
            Node child = nodes.get(edge.getSource());
            Node parent = nodes.get(edge.getTarget());
            if (child == null || parent == null) continue;
            segments.add(new Segment(child, parent, edge));
        }
        return segments;
    }

    static String label(Node node) {
        if (node == null) return "";
        return node.getCode() == null || node.getCode().isEmpty() ? node.getName()
            : node.getCode() + " " + node.getName();
    }

    /** 名称（编码）：用于句子里需要“是什么”的位置。 */
    static String named(Node node) {
        if (node == null) return "";
        String name = node.getName() == null || node.getName().isEmpty() ? node.getCode() : node.getName();
        return node.getCode() == null || node.getCode().isEmpty() ? name
            : name + "（" + node.getCode() + "）";
    }

    static String reasonText(String reason) {
        if (reason == null) return "路径结束";
        switch (reason) {
            case "finished_product":
                return "整机成品";
            case "no_parent":
                return "该物料没有更上级的启用装配记录，当前追溯到此";
            case "depth_limit":
                return "已达到 " + MAX_DEPTH + " 层查询上限，不表示没有更上级";
            case "cycle":
                return "装配关系出现回环，只呈现已确认的非循环段";
            case "result_limit":
                return "已达到返回路径上限，不表示没有更多路径";
            default:
                return reason;
        }
    }

    static String quantity(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? 0 : Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try {
            return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static Node nodeOfItem(Map<String, Object> row) {
        boolean product = "PRODUCT".equalsIgnoreCase(text(row.get("itemOrProduct")));
        return new Node(product ? "Product" : "Item", text(row.get("itemCode")), text(row.get("itemName")),
            text(row.get("itemTypeName")), text(row.get("unit")), FINISHED_TYPE.equals(text(row.get("itemTypeCode"))));
    }

    private static Node nodeOfParent(Map<String, Object> row) {
        boolean product = "PRODUCT".equalsIgnoreCase(text(row.get("parentKind")));
        return new Node(product ? "Product" : "Item", text(row.get("parentCode")), text(row.get("parentName")),
            text(row.get("parentTypeName")), text(row.get("parentUnit")),
            FINISHED_TYPE.equals(text(row.get("parentTypeCode"))));
    }

    private static String nodeIdOfProduct(Map<String, Object> row, String prefix) {
        boolean product = "PRODUCT".equalsIgnoreCase(text(row.get(prefix + "Kind")));
        return (product ? "Product:" : "Item:") + text(row.get(prefix + "Code"));
    }

    private static String itemCode(String value) {
        String text = value == null ? "" : value.trim();
        if (!CODE.matcher(text).matches()) {
            throw new ServiceException("请提供完整的物料编码（字母、数字、点、下划线或短横线，最多 64 位）。",
                HttpStatus.BAD_REQUEST);
        }
        return text;
    }

    private static List<Long> via(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException("请提供当前路径截至该段的 BOM 行号。", HttpStatus.BAD_REQUEST);
        }
        String[] parts = value.split(",");
        if (parts.length > MAX_VIA) {
            throw new ServiceException("路径最多校验 " + MAX_VIA + " 段装配。", HttpStatus.BAD_REQUEST);
        }
        List<Long> rows = new ArrayList<>();
        for (String part : parts) {
            String text = part.trim();
            if (!text.matches("\\d{1,19}")) {
                throw new ServiceException("BOM 行号应为数字，多段用逗号分隔。", HttpStatus.BAD_REQUEST);
            }
            rows.add(Long.parseLong(text));
        }
        return rows;
    }

    private static List<String> codes(List<Map<String, Object>> items) {
        return items.stream().map(item -> text(item.get("itemCode"))).filter(code -> !code.isEmpty())
            .distinct().collect(Collectors.toList());
    }

    private static ServiceException changed() {
        return new ServiceException("装配关系已变化，请刷新路径。", HttpStatus.CONFLICT);
    }

    private void requireBomRead() {
        if (!permissions.hasPermi("mes:md:mditem:list")) {
            throw new ServiceException("当前账号没有物料清单的查询权限。", HttpStatus.FORBIDDEN);
        }
    }

    private void requireBomRow() {
        if (!permissions.hasPermi("mes:md:mditem:query")) {
            throw new ServiceException("当前账号没有 BOM 行详情的查询权限。", HttpStatus.FORBIDDEN);
        }
    }

    // ------------------------------------------------------------------ 数据结构

    @Getter
    public static class Impact {
        private final String originId;
        private final List<Node> nodes = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final List<Path> paths = new ArrayList<>();
        private final List<String> limitReasons = new ArrayList<>();
        private boolean limited;

        Impact(String originId) {
            this.originId = originId;
        }

        void finish(Set<String> reasons) {
            for (String reason : reasons) {
                if (!limitReasons.contains(reason)) limitReasons.add(reason);
            }
            limited = limited || !limitReasons.isEmpty();
        }
    }

    @Getter
    public static class Node {
        private final String id;
        private final String type;
        private final String code;
        private final String name;
        private final String categoryName;
        private final String unit;
        @JsonIgnore
        private final boolean finished;

        Node(String type, String code, String name, String categoryName, String unit, boolean finished) {
            this.type = type;
            this.id = type + ":" + code;
            this.code = code;
            this.name = name;
            this.categoryName = categoryName;
            this.unit = unit;
            this.finished = finished;
        }
    }

    @Getter
    public static class Edge {
        private final String id;
        private final String bomId;
        private final String kind;
        private final String source;
        private final String target;
        private final BigDecimal quantity;
        private final String childUnit;
        private final String parentUnit;

        Edge(String id, String bomId, String kind, String source, String target, BigDecimal quantity,
             String childUnit, String parentUnit) {
            this.id = id;
            this.bomId = bomId;
            this.kind = kind;
            this.source = source;
            this.target = target;
            this.quantity = quantity;
            this.childUnit = childUnit;
            this.parentUnit = parentUnit;
        }
    }

    @Getter
    public static class Path {
        private final List<String> edgeIds;
        private final String terminalId;
        private final String stopReason;

        Path(List<String> edgeIds, String terminalId, String stopReason) {
            this.edgeIds = edgeIds;
            this.terminalId = terminalId;
            this.stopReason = stopReason;
        }
    }

    @Getter
    public static class Segment {
        private final Node child;
        private final Node parent;
        private final Edge edge;

        Segment(Node child, Node parent, Edge edge) {
            this.child = child;
            this.parent = parent;
            this.edge = edge;
        }
    }

    @Getter
    public static class EdgeDetail {
        private final Map<String, Object> bomRow;
        private final String evidenceStatus;
        private final List<Map<String, Object>> sources;

        EdgeDetail(Map<String, Object> bomRow, String evidenceStatus, List<Map<String, Object>> sources) {
            this.bomRow = bomRow;
            this.evidenceStatus = evidenceStatus;
            this.sources = sources;
        }
    }

    @Getter
    public static class Intent {
        private final String itemCode;
        private final List<String> candidates;
        private final String message;

        Intent(String itemCode, List<String> candidates, String message) {
            this.itemCode = itemCode;
            this.candidates = candidates;
            this.message = message;
        }
    }

    @Getter
    public static class Answer {
        private final String itemCode;
        private final Impact impact;
        private final List<Map<String, Object>> citations;
        private final String facts;
        private final String summary;

        Answer(String itemCode, Impact impact, List<Map<String, Object>> citations, String facts, String summary) {
            this.itemCode = itemCode;
            this.impact = impact;
            this.citations = citations;
            this.facts = facts;
            this.summary = summary;
        }
    }

    private static class Link {
        private final long parentId;
        private final Edge edge;
        private final boolean finished;

        Link(long parentId, Edge edge, boolean finished) {
            this.parentId = parentId;
            this.edge = edge;
            this.finished = finished;
        }
    }
}
