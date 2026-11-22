package com.ktg.mes.pro.chain;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 贯通单据清理：按工单（含半成品子工单）级联删除生产与仓储单据，并按库存流水反冲库存。
 *
 * 只清理生产链路（任务/报工/过程检验/产出/消耗/领料/成品入库），
 * 不删除销售出库、发货通知、出货检验等对外单据，也不删除物料主数据与批次。
 */
@Service
public class ChainCleanupService {

    /** 贯通工单识别口径：备注含“全流程贯通”，或编码为本流程生成的时间戳-MO。 */
    private static final String CHAIN_MATCH =
            "(w.remark like '%全流程贯通%' or w.workorder_code like '%-MO')";

    /** 需要按 workorder_id 直接删除的表（无子表或子表已单独处理）。 */
    private static final List<String> WORKORDER_TABLES = Arrays.asList(
            "pro_feedback", "pro_task", "pro_workorder_bom",
            "pro_card", "pro_andon_record", "pro_task_issue",
            "pro_trans_order", "pro_trans_consume",
            "wm_package_line", "wm_materialrequest_notice", "wm_pback_header",
            "wm_rt_issue", "wm_outsource_issue", "wm_outsource_recpt");

    private final JdbcTemplate jdbc;

    public ChainCleanupService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ 查询

    public List<Map<String, Object>> candidates(String keyword, boolean onlyChain) {
        StringBuilder sql = new StringBuilder(
                "select w.workorder_id, w.workorder_code, w.workorder_name, w.status, w.parent_id, "
                        + "w.parent_id as parentId, w.quantity, w.create_time, w.create_by, w.remark, "
                        + "case when " + CHAIN_MATCH + " then 1 else 0 end as chain_flag, "
                        + "(select count(*) from pro_task t where t.workorder_id = w.workorder_id) as task_count, "
                        + "(select count(*) from pro_feedback f where f.workorder_id = w.workorder_id) as feedback_count, "
                        + "(select count(*) from qc_ipqc q where q.workorder_id = w.workorder_id) as ipqc_count, "
                        + "(select count(*) from wm_issue_header i where i.workorder_id = w.workorder_id) as issue_count, "
                        + "(select count(*) from wm_product_recpt r where r.workorder_id = w.workorder_id) as recpt_count "
                        + "from pro_workorder w where 1 = 1");
        List<Object> args = new ArrayList<Object>();
        if (onlyChain) {
            sql.append(" and ").append(CHAIN_MATCH);
        }
        if (StringUtils.isNotEmpty(keyword)) {
            sql.append(" and (w.workorder_code like ? or w.workorder_name like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        sql.append(" order by w.workorder_id desc limit 500");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> preview(List<Long> workorderIds) {
        Set<Long> ids = expand(workorderIds);
        Docs docs = collect(ids);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("workorderIds", new ArrayList<Long>(ids));
        result.put("workorders", workorders(ids));
        result.put("childCount", ids.size() - new LinkedHashSet<Long>(workorderIds).size());
        Map<String, Object> counts = counts(docs);
        result.put("counts", counts);
        long total = 0;
        for (Object value : counts.values()) {
            if (value instanceof Number) {
                total += ((Number) value).longValue();
            }
        }
        result.put("totalRows", total);
        result.put("stockImpact", stockImpact(docs));
        result.put("warnings", warnings(ids, docs, workorderIds));
        return result;
    }

    // ------------------------------------------------------------------ 执行

    /**
     * 级联删除贯通工单及其派生单据；库存按流水反冲（逐行夹到 0）。
     * 调用方必须传入 confirmText=DELETE，避免误触发。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(List<Long> workorderIds, String confirmText) {
        if (!"DELETE".equals(confirmText)) {
            throw new IllegalArgumentException("请输入 DELETE 以确认删除");
        }
        Set<Long> ids = expand(workorderIds);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("没有可删除的工单");
        }
        Docs docs = collect(ids);
        Map<String, Object> summary = new LinkedHashMap<String, Object>();

        // 1) 反冲库存：被删单据的流水带符号取反，逐库存行夹到 0
        int stockRows = reverseStock(docs);
        summary.put("stockRows", stockRows);
        summary.put("stockImpact", stockImpact(docs));

        // 2) 库存流水
        int txn = deleteByDocIds("wm_transaction", docs);
        summary.put("wm_transaction", txn);

        // 3) 子表
        summary.put("qc_ipqc_line", deleteByIds("qc_ipqc_line", "ipqc_id", docs.ipqcIds));
        summary.put("qc_defect_record", deleteByIds("qc_defect_record", "qc_id", docs.ipqcIds));
        summary.put("wm_issue_line", deleteByIds("wm_issue_line", "issue_id", docs.issueIds));
        summary.put("wm_issue_detail", deleteByIds("wm_issue_detail", "issue_id", docs.issueIds));
        summary.put("wm_item_consume_line", deleteByIds("wm_item_consume_line", "record_id", docs.consumeIds));
        summary.put("wm_item_consume_detail", deleteByIds("wm_item_consume_detail", "record_id", docs.consumeIds));
        summary.put("wm_product_produce_line", deleteByIds("wm_product_produce_line", "record_id", docs.produceIds));
        summary.put("wm_product_produce_detail", deleteByIds("wm_product_produce_detail", "record_id", docs.produceIds));
        summary.put("wm_product_recpt_line", deleteByIds("wm_product_recpt_line", "recpt_id", docs.recptIds));
        summary.put("pro_card_process", deleteByIds("pro_card_process", "card_id", docs.cardIds));
        summary.put("wm_rt_issue_line", deleteByIds("wm_rt_issue_line", "rt_id", docs.rtIds));
        summary.put("wm_outsource_issue_line", deleteByIds("wm_outsource_issue_line", "issue_id", docs.outIssueIds));
        summary.put("wm_outsource_issue_detail", deleteByIds("wm_outsource_issue_detail", "issue_id", docs.outIssueIds));
        summary.put("wm_outsource_recpt_line", deleteByIds("wm_outsource_recpt_line", "recpt_id", docs.outRecptIds));
        summary.put("wm_outsource_recpt_detail", deleteByIds("wm_outsource_recpt_detail", "recpt_id", docs.outRecptIds));

        // 4) 单据头
        summary.put("qc_ipqc", deleteByIds("qc_ipqc", "ipqc_id", docs.ipqcIds));
        summary.put("wm_issue_header", deleteByIds("wm_issue_header", "issue_id", docs.issueIds));
        summary.put("wm_item_consume", deleteByIds("wm_item_consume", "record_id", docs.consumeIds));
        summary.put("wm_product_produce", deleteByIds("wm_product_produce", "record_id", docs.produceIds));
        summary.put("wm_product_recpt", deleteByIds("wm_product_recpt", "recpt_id", docs.recptIds));
        summary.put("wm_rt_issue", deleteByIds("wm_rt_issue", "rt_id", docs.rtIds));
        summary.put("wm_outsource_issue", deleteByIds("wm_outsource_issue", "issue_id", docs.outIssueIds));
        summary.put("wm_outsource_recpt", deleteByIds("wm_outsource_recpt", "recpt_id", docs.outRecptIds));

        // 5) 生产单据与工单本身
        for (String table : WORKORDER_TABLES) {
            summary.put(table, deleteByWorkorder("`" + table + "`", ids));
        }
        int removedWorkorders = deleteByWorkorder("pro_workorder", ids);
        summary.put("pro_workorder", removedWorkorders);
        summary.put("workorderIds", new ArrayList<Long>(ids));
        summary.put("childCount", ids.size() - new LinkedHashSet<Long>(workorderIds).size());
        return summary;
    }

    // ------------------------------------------------------------------ 内部

    private Set<Long> expand(Collection<Long> roots) {
        Set<Long> ids = new LinkedHashSet<Long>();
        for (Long id : roots) {
            if (id != null) {
                ids.add(id);
            }
        }
        boolean changed = ids.size() > 0;
        while (changed) {
            changed = false;
            List<Long> children = jdbc.queryForList(
                    "select workorder_id from pro_workorder where parent_id in (" + marks(ids) + ")",
                    Long.class, ids.toArray());
            for (Long child : children) {
                if (ids.add(child)) {
                    changed = true;
                }
            }
        }
        return ids;
    }

    private List<Map<String, Object>> workorders(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbc.queryForList(
                "select workorder_id, workorder_code, workorder_name, status, parent_id, quantity, create_time "
                        + "from pro_workorder where workorder_id in (" + marks(ids) + ") order by workorder_id",
                ids.toArray());
    }

    private Docs collect(Set<Long> ids) {
        Docs docs = new Docs();
        docs.workorderCount = ids.size();
        if (ids.isEmpty()) {
            return docs;
        }
        Object[] args = ids.toArray();
        String in = marks(ids);
        docs.taskIds = jdbc.queryForList("select task_id from pro_task where workorder_id in (" + in + ")", Long.class, args);
        docs.feedbackIds = jdbc.queryForList("select record_id from pro_feedback where workorder_id in (" + in + ")", Long.class, args);
        docs.ipqcIds = jdbc.queryForList("select ipqc_id from qc_ipqc where workorder_id in (" + in + ")", Long.class, args);
        docs.issueIds = jdbc.queryForList("select issue_id from wm_issue_header where workorder_id in (" + in + ")", Long.class, args);
        docs.consumeIds = jdbc.queryForList("select record_id from wm_item_consume where workorder_id in (" + in + ")", Long.class, args);
        docs.produceIds = jdbc.queryForList("select record_id from wm_product_produce where workorder_id in (" + in + ")", Long.class, args);
        docs.recptIds = jdbc.queryForList("select recpt_id from wm_product_recpt where workorder_id in (" + in + ")", Long.class, args);
        docs.cardIds = jdbc.queryForList("select card_id from pro_card where workorder_id in (" + in + ")", Long.class, args);
        docs.rtIds = jdbc.queryForList("select rt_id from wm_rt_issue where workorder_id in (" + in + ")", Long.class, args);
        docs.outIssueIds = jdbc.queryForList("select issue_id from wm_outsource_issue where workorder_id in (" + in + ")", Long.class, args);
        docs.outRecptIds = jdbc.queryForList("select recpt_id from wm_outsource_recpt where workorder_id in (" + in + ")", Long.class, args);
        docs.lineCounts.put("qc_ipqc_line", countByIds("qc_ipqc_line", "ipqc_id", docs.ipqcIds));
        docs.lineCounts.put("wm_issue_line", countByIds("wm_issue_line", "issue_id", docs.issueIds));
        docs.lineCounts.put("wm_issue_detail", countByIds("wm_issue_detail", "issue_id", docs.issueIds));
        docs.lineCounts.put("wm_item_consume_line", countByIds("wm_item_consume_line", "record_id", docs.consumeIds));
        docs.lineCounts.put("wm_item_consume_detail", countByIds("wm_item_consume_detail", "record_id", docs.consumeIds));
        docs.lineCounts.put("wm_product_produce_line", countByIds("wm_product_produce_line", "record_id", docs.produceIds));
        docs.lineCounts.put("wm_product_produce_detail", countByIds("wm_product_produce_detail", "record_id", docs.produceIds));
        docs.lineCounts.put("wm_product_recpt_line", countByIds("wm_product_recpt_line", "recpt_id", docs.recptIds));
        docs.transactionIds = transactionCount(docs);
        return docs;
    }

    private Map<String, Object> counts(Docs docs) {
        Map<String, Object> counts = new LinkedHashMap<String, Object>();
        counts.put("pro_workorder", docs.workorderCount);
        counts.put("pro_task", docs.taskIds.size());
        counts.put("pro_feedback", docs.feedbackIds.size());
        counts.put("qc_ipqc", docs.ipqcIds.size());
        counts.put("wm_issue_header", docs.issueIds.size());
        counts.put("wm_item_consume", docs.consumeIds.size());
        counts.put("wm_product_produce", docs.produceIds.size());
        counts.put("wm_product_recpt", docs.recptIds.size());
        counts.putAll(docs.lineCounts);
        counts.put("wm_transaction", docs.transactionIds);
        return counts;
    }

    private Map<String, Object> counts(Set<Long> ids) {
        Docs docs = collect(ids);
        docs.workorderCount = ids.size();
        return counts(docs);
    }

    private List<Map<String, Object>> stockImpact(Docs docs) {
        String where = transactionWhere(docs);
        if (where == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select s.item_code, s.item_name, s.warehouse_code, sum(t.transaction_quantity) as delta, "
                        + "count(*) as txn_rows from wm_transaction t "
                        + "join wm_material_stock s on s.material_stock_id = t.material_stock_id "
                        + "where " + where + " group by t.material_stock_id, s.item_code, s.item_name, s.warehouse_code "
                        + "having abs(sum(t.transaction_quantity)) > 0 order by s.warehouse_code, s.item_code",
                transactionArgs(docs));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<String, Object>(row);
            Object delta = row.get("delta");
            if (delta instanceof Number) {
                item.put("restore", new BigDecimal(String.valueOf(delta)).negate());
            }
            result.add(item);
        }
        return result;
    }

    private List<String> warnings(Set<Long> ids, Docs docs) {
        return warnings(ids, docs, ids);
    }

    private List<String> warnings(Set<Long> ids, Docs docs, Collection<Long> roots) {
        List<String> warnings = new ArrayList<String>();
        Set<Long> rootSet = new LinkedHashSet<Long>();
        if (roots != null) {
            for (Long id : roots) {
                if (id != null) {
                    rootSet.add(id);
                }
            }
        }
        int autoChildren = 0;
        int rootChildren = 0;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select workorder_id, parent_id from pro_workorder where workorder_id in (" + marks(ids) + ")",
                ids.toArray());
        for (Map<String, Object> row : rows) {
            Object parent = row.get("parent_id");
            boolean isChild = parent != null && Long.valueOf(String.valueOf(parent)) > 0;
            if (!isChild) {
                continue;
            }
            Object id = row.get("workorder_id");
            boolean root = id != null && rootSet.contains(Long.valueOf(String.valueOf(id)));
            if (root) {
                rootChildren++;
            } else {
                autoChildren++;
            }
        }
        if (autoChildren > 0) {
            warnings.add("将自动带入并删除 " + autoChildren + " 张半成品子工单");
        }
        if (rootChildren > 0) {
            warnings.add("所选工单中有 " + rootChildren + " 张本身就是半成品子工单，删除后父工单的投入会相应减少");
        }
        long finished = jdbc.queryForObject(
                "select count(*) from pro_workorder where workorder_id in (" + marks(ids) + ") and status = ?",
                Long.class, concat(ids.toArray(), UserConstants.ORDER_STATUS_FINISHED));
        if (finished > 0) {
            warnings.add("其中 " + finished + " 张工单已是「已完成」，删除后成品入库数量随之回退");
        }
        if (!docs.recptIds.isEmpty()) {
            Long sold = jdbc.queryForObject(
                    "select count(*) from wm_product_sales_line l where l.batch_id in ("
                            + "select rl.batch_id from wm_product_recpt_line rl where rl.recpt_id in (" + marks(docs.recptIds) + ") "
                            + "and rl.batch_id is not null)",
                    Long.class, docs.recptIds.toArray());
            if (sold != null && sold > 0) {
                warnings.add("检测到 " + sold + " 行成品已被销售出库：本功能不删除销售/发货单据，成品仓只会回退到 0，不会出现负库存");
            }
        }
        long pending = jdbc.queryForObject(
                "select count(*) from pro_workorder where workorder_id in (" + marks(ids) + ") and status not in (?,?)",
                Long.class, concat(ids.toArray(), UserConstants.ORDER_STATUS_PREPARE, "CANCELED"));
        if (pending > 0) {
            warnings.add("其中 " + pending + " 张工单已确认或已完工，删除会一并清除其报工/检验/出入库记录");
        }
        return warnings;
    }

    private int reverseStock(Docs docs) {
        String where = transactionWhere(docs);
        if (where == null) {
            return 0;
        }
        String sql = "update wm_material_stock s join ("
                + "select material_stock_id, sum(transaction_quantity) as delta from wm_transaction where " + where
                + " group by material_stock_id) x on x.material_stock_id = s.material_stock_id "
                + "set s.quantity_onhand = greatest(0, s.quantity_onhand - x.delta), s.update_time = now()";
        return jdbc.update(sql, transactionArgs(docs));
    }

    private int transactionCount(Docs docs) {
        String where = transactionWhere(docs);
        if (where == null) {
            return 0;
        }
        Long count = jdbc.queryForObject("select count(*) from wm_transaction where " + where,
                Long.class, transactionArgs(docs));
        return count == null ? 0 : count.intValue();
    }

    private String transactionWhere(Docs docs) {
        List<String> parts = new ArrayList<String>();
        if (!docs.issueIds.isEmpty()) {
            parts.add("(source_doc_type = 'ISSUE' and source_doc_id in (" + marks(docs.issueIds) + "))");
        }
        if (!docs.consumeIds.isEmpty()) {
            parts.add("(source_doc_type = 'ITEM_CONSUME' and source_doc_id in (" + marks(docs.consumeIds) + "))");
        }
        if (!docs.produceIds.isEmpty()) {
            parts.add("(source_doc_type = 'PRODUCT_PRODUCT' and source_doc_id in (" + marks(docs.produceIds) + "))");
        }
        if (!docs.recptIds.isEmpty()) {
            parts.add("(source_doc_type = 'PRODUCT_RECPT' and source_doc_id in (" + marks(docs.recptIds) + "))");
        }
        return parts.isEmpty() ? null : "(" + String.join(" or ", parts) + ")";
    }

    private Object[] transactionArgs(Docs docs) {
        List<Object> args = new ArrayList<Object>();
        args.addAll(docs.issueIds);
        args.addAll(docs.consumeIds);
        args.addAll(docs.produceIds);
        args.addAll(docs.recptIds);
        return args.toArray();
    }

    private int deleteByDocIds(String table, Docs docs) {
        int rows = 0;
        rows += deleteByIds(table, "source_doc_id", docs.issueIds, "source_doc_type", "ISSUE");
        rows += deleteByIds(table, "source_doc_id", docs.consumeIds, "source_doc_type", "ITEM_CONSUME");
        rows += deleteByIds(table, "source_doc_id", docs.produceIds, "source_doc_type", "PRODUCT_PRODUCT");
        rows += deleteByIds(table, "source_doc_id", docs.recptIds, "source_doc_type", "PRODUCT_RECPT");
        return rows;
    }

    private int deleteByIds(String table, String column, List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        return jdbc.update("delete from `" + table + "` where " + column + " in (" + marks(ids) + ")", ids.toArray());
    }

    private int deleteByIds(String table, String column, List<Long> ids, String extraColumn, String extraValue) {
        if (ids.isEmpty()) {
            return 0;
        }
        List<Object> args = new ArrayList<Object>(ids);
        args.add(extraValue);
        return jdbc.update("delete from `" + table + "` where " + column + " in (" + marks(ids) + ") and " + extraColumn + " = ?",
                args.toArray());
    }

    private int deleteByWorkorder(String table, Set<Long> ids) {
        return jdbc.update("delete from " + table + " where workorder_id in (" + marks(ids) + ")", ids.toArray());
    }

    private int countByIds(String table, String column, List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        Long count = jdbc.queryForObject("select count(*) from `" + table + "` where " + column + " in (" + marks(ids) + ")",
                Long.class, ids.toArray());
        return count == null ? 0 : count.intValue();
    }

    private String marks(Collection<?> items) {
        if (items.isEmpty()) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
        return sb.toString();
    }

    private Object[] concat(Object[] base, Object... extra) {
        Object[] result = new Object[base.length + extra.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(extra, 0, result, base.length, extra.length);
        return result;
    }

    /** 一次清理涉及的单据主键集合。 */
    private static final class Docs {
        List<Long> taskIds = Collections.emptyList();
        List<Long> feedbackIds = Collections.emptyList();
        List<Long> ipqcIds = Collections.emptyList();
        List<Long> issueIds = Collections.emptyList();
        List<Long> consumeIds = Collections.emptyList();
        List<Long> produceIds = Collections.emptyList();
        List<Long> recptIds = Collections.emptyList();
        List<Long> cardIds = Collections.emptyList();
        List<Long> rtIds = Collections.emptyList();
        List<Long> outIssueIds = Collections.emptyList();
        List<Long> outRecptIds = Collections.emptyList();
        int transactionIds = 0;
        int workorderCount = 0;
        Map<String, Integer> lineCounts = new LinkedHashMap<String, Integer>();
    }
}
