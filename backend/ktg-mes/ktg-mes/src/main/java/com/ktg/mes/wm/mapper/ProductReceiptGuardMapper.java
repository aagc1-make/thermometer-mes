package com.ktg.mes.wm.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 产品入库（工单完工 → 成品仓）的来源可信性与累计上限查询。
 *
 * <p>来源口径（待业务确认，已按“最后产出工序 + 包装完成放行”实现）：</p>
 * <ul>
 *   <li>“最后产出工序” = 该工单成品( item_id = 产品 )的合格产出行中，
 *       产出行所属 {@code pro_route_process.order_num} 最大的一道工序；
 *       同一产品在中间工序的 OK 产出不计入；</li>
 *   <li>“包装完成放行” 由工单 FINISHED + 产出行 {@code status='FINISHED'} 共同体现
 *       （工单只有在其全部工序任务（含包装入库）完成后才会 FINISHED）；</li>
 *   <li>不修改历史产出记录：源数据仍在原工序上，仅按上述规则选取。</li>
 * </ul>
 */
public interface ProductReceiptGuardMapper {

    /**
     * 工单成品的产出来源工序（distinct，仅统计 FINISHED + OK 行）。
     * 应恰好一道；0 道 = 无产出，多道 = 口径不确定，调用方必须拒绝或转人工。
     */
    @Select("select distinct p.process_id from wm_product_produce p "
            + "join wm_product_produce_line l on l.record_id=p.record_id "
            + "where p.workorder_id=#{workorderId} and l.item_id=#{productId} and l.quality_status='OK' "
            + "and p.status='FINISHED'")
    List<Long> produceProcessesForProduct(@Param("workorderId") Long workorderId,
                                          @Param("productId") Long productId);

    /** 产品工艺路线的终末工序（next_process_id=0）。 */
    @Select("select rp.process_id from pro_route_process rp "
            + "join pro_route_product rpt on rpt.route_id=rp.route_id "
            + "where rpt.item_id=#{productId} and rp.next_process_id=0 "
            + "order by rp.order_num desc, rp.process_id desc limit 1")
    Long terminalProcessId(@Param("productId") Long productId);

    /** 工单在指定工序上已完成（FINISHED）的报工条数（入库放行证据）。 */
    @Select("select count(*) from pro_feedback where workorder_id=#{workorderId} "
            + "and process_id=#{processId} and status='FINISHED'")
    int countFinishedFeedbackAtProcess(@Param("workorderId") Long workorderId,
                                       @Param("processId") Long processId);

    /** 工单成品在指定工序上已放行的合格产出，按批次汇总。 */
    @Select("select coalesce(l.batch_code,'') as batchCode, l.batch_id as batchId, "
            + "coalesce(sum(l.quantity_produce),0) as quantity "
            + "from wm_product_produce_line l join wm_product_produce p on p.record_id=l.record_id "
            + "where p.workorder_id=#{workorderId} and l.item_id=#{productId} and l.quality_status='OK' "
            + "and p.process_id=#{processId} and p.status='FINISHED' "
            + "group by l.batch_code, l.batch_id")
    List<Map<String, Object>> qualifiedOutputByBatch(@Param("workorderId") Long workorderId,
                                                     @Param("productId") Long productId,
                                                     @Param("processId") Long processId);

    /** 指定工单+物料+批次的工序合格产出合计（执行过账时按批次校验上限）。 */
    @Select("select coalesce(sum(l.quantity_produce),0) from wm_product_produce_line l "
            + "join wm_product_produce p on p.record_id=l.record_id "
            + "where p.workorder_id=#{workorderId} and l.item_id=#{itemId} and l.quality_status='OK' "
            + "and p.process_id=#{processId} and p.status='FINISHED' "
            + "and coalesce(l.batch_code,'')=coalesce(#{batchCode},'')")
    BigDecimal qualifiedOutputForBatch(@Param("workorderId") Long workorderId,
                                       @Param("itemId") Long itemId,
                                       @Param("batchCode") String batchCode,
                                       @Param("processId") Long processId);

    /** 指定工单+批次已经执行过账（FINISHED）的产品入库累计量。 */
    @Select("select coalesce(sum(l.quantity_recived),0) from wm_product_recpt_line l "
            + "join wm_product_recpt r on r.recpt_id=l.recpt_id "
            + "where r.workorder_id=#{workorderId} and r.status='FINISHED' "
            + "and coalesce(l.batch_code,'')=coalesce(#{batchCode},'')")
    BigDecimal finishedProductRecptByBatch(@Param("workorderId") Long workorderId,
                                           @Param("batchCode") String batchCode);

    /** 指定工单+批次尚未过账的有效草稿占用（排除已执行/已取消；排除当前入库单）。 */
    @Select("select coalesce(sum(l.quantity_recived),0) from wm_product_recpt_line l "
            + "join wm_product_recpt r on r.recpt_id=l.recpt_id "
            + "where r.workorder_id=#{workorderId} and r.status not in ('FINISHED','CANCELED') "
            + "and coalesce(l.batch_code,'')=coalesce(#{batchCode},'') "
            + "and r.recpt_id<>coalesce(#{excludeRecptId},-1)")
    BigDecimal occupiedProductRecptByBatch(@Param("workorderId") Long workorderId,
                                           @Param("batchCode") String batchCode,
                                           @Param("excludeRecptId") Long excludeRecptId);

    /** 按幂等键查是否已存在该来源批次的产品入库草稿。 */
    @Select("select count(*) from wm_product_recpt where attr1=#{linkageKey}")
    int countProductRecptByLinkageKey(@Param("linkageKey") String linkageKey);

    /** 工单在终末工序(包装)有效完成的报工数量合计（总报工 − 不合格，作为放行数量上限依据）。 */
    @Select("select coalesce(sum(quantity_feedback),0) - coalesce(sum(quantity_unquanlified),0) "
            + "from pro_feedback "
            + "where workorder_id=#{workorderId} and process_id=#{processId} and status='FINISHED'")
    BigDecimal completedPackagingQuantity(@Param("workorderId") Long workorderId,
                                          @Param("processId") Long processId);

    /** 工单在指定工序上的合格产出合计（跨批次）。 */
    @Select("select coalesce(sum(l.quantity_produce),0) from wm_product_produce_line l "
            + "join wm_product_produce p on p.record_id=l.record_id "
            + "where p.workorder_id=#{workorderId} and l.item_id=#{productId} and l.quality_status='OK' "
            + "and p.process_id=#{processId} and p.status='FINISHED'")
    BigDecimal totalQualifiedOutput(@Param("workorderId") Long workorderId,
                                    @Param("productId") Long productId,
                                    @Param("processId") Long processId);

    /** 工单已执行过账（FINISHED）的产品入库累计量（跨批次）。 */
    @Select("select coalesce(sum(l.quantity_recived),0) from wm_product_recpt_line l "
            + "join wm_product_recpt r on r.recpt_id=l.recpt_id "
            + "where r.workorder_id=#{workorderId} and r.status='FINISHED'")
    BigDecimal finishedProductRecptTotal(@Param("workorderId") Long workorderId);
}
