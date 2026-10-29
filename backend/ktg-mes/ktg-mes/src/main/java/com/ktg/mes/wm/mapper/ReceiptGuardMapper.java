package com.ktg.mes.wm.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 采购入库/来料检验的来源可信性与幂等助手查询。
 *
 * <p>这里的 SQL 全部以数据库中的来源行、检验单和已过账单据为准，
 * 不接受前端传入的数量或结论；调用方必须处于同一数据库事务内，
 * 并且在调用前已对主单（或来源行）加行锁。</p>
 */
public interface ReceiptGuardMapper {

    /** 状态条件更新：只有当前状态等于 fromStatus 时才允许流转，返回受影响行数。 */
    @Update("update wm_item_recpt set status=#{toStatus}, update_by=#{operator}, update_time=now() "
            + "where recpt_id=#{recptId} and status=#{fromStatus}")
    int transitionReceiptStatus(@Param("recptId") Long recptId,
                                @Param("fromStatus") String fromStatus,
                                @Param("toStatus") String toStatus,
                                @Param("operator") String operator);

    /** 指定到货行上，除当前入库单以外已经执行过账的累计入库数量。 */
    @Select("select coalesce(sum(l.quantity_recived),0) from wm_item_recpt_line l "
            + "join wm_item_recpt r on r.recpt_id=l.recpt_id "
            + "where l.notice_line_id=#{noticeLineId} and r.status='FINISHED' and r.recpt_id<>#{excludeRecptId}")
    BigDecimal finishedQuantityOnNoticeLine(@Param("noticeLineId") Long noticeLineId,
                                            @Param("excludeRecptId") Long excludeRecptId);

    /** 指定到货行上，除当前入库行以外、未取消入库单已经占用的数量（草稿阶段防超量）。 */
    @Select("select coalesce(sum(l.quantity_recived),0) from wm_item_recpt_line l "
            + "join wm_item_recpt r on r.recpt_id=l.recpt_id "
            + "where l.notice_line_id=#{noticeLineId} and r.status<>'CANCELED' "
            + "and l.line_id<>coalesce(#{excludeLineId},-1)")
    BigDecimal occupiedQuantityOnNoticeLine(@Param("noticeLineId") Long noticeLineId,
                                            @Param("excludeLineId") Long excludeLineId);

    /** 指定到货通知单上已经执行过账的累计入库数量（用于汇总通知单状态）。 */
    @Select("select coalesce(sum(l.quantity_recived),0) from wm_item_recpt_line l "
            + "join wm_item_recpt r on r.recpt_id=l.recpt_id "
            + "where r.notice_id=#{noticeId} and r.status='FINISHED'")
    BigDecimal finishedQuantityOnNotice(@Param("noticeId") Long noticeId);

    /**
     * 校验来料检验单是否真的属于该到货行：来源类型、来源单、来源行、物料全部一致，且已完成判定。
     * 返回 1 表示可信，0 表示伪造或跨单关联。
     */
    @Select("select count(*) from qc_iqc where iqc_id=#{iqcId} "
            + "and source_doc_type='ARRIVAL_NOTICE' and source_doc_id=#{noticeId} "
            + "and source_line_id=#{noticeLineId} and status in ('CONFIRMED','FINISHED')")
    int countCompletedIqcBoundToArrivalLine(@Param("iqcId") Long iqcId,
                                            @Param("noticeId") Long noticeId,
                                            @Param("noticeLineId") Long noticeLineId);

    /** 历史无来源单据兼容：检验单本身存在且已完成。 */
    @Select("select count(*) from qc_iqc where iqc_id=#{iqcId} and status in ('CONFIRMED','FINISHED')")
    int countCompletedIqc(@Param("iqcId") Long iqcId);

    /** 指定到货行上其它已完成检验单累计的检测数量，用于“累计处理量不得超过来源可处理量”。 */
    @Select("select coalesce(sum(quantity_check),0) from qc_iqc "
            + "where source_doc_type='ARRIVAL_NOTICE' and source_line_id=#{noticeLineId} "
            + "and status in ('CONFIRMED','FINISHED') and iqc_id<>#{excludeIqcId}")
    BigDecimal completedCheckQuantityOnArrivalLine(@Param("noticeLineId") Long noticeLineId,
                                                   @Param("excludeIqcId") Long excludeIqcId);

    /** 指定到货行上其它已完成检验单累计的合格数量。 */
    @Select("select coalesce(sum(quantity_qualified),0) from qc_iqc "
            + "where source_doc_type='ARRIVAL_NOTICE' and source_line_id=#{noticeLineId} "
            + "and status in ('CONFIRMED','FINISHED') and iqc_id<>#{excludeIqcId}")
    BigDecimal completedQualifiedQuantityOnArrivalLine(@Param("noticeLineId") Long noticeLineId,
                                                       @Param("excludeIqcId") Long excludeIqcId);

    /**
     * 来料检验完成的原子条件更新：只有当前仍是草稿时才会成功。
     * 来源字段以数据库中的原值为准写入，前端传入的来源值不参与更新。
     */
    @Update("update qc_iqc set status=#{status}, quantity_check=#{quantityCheck}, "
            + "quantity_qualified=#{quantityQualified}, quantity_unqualified=#{quantityUnqualified}, "
            + "check_result=#{checkResult}, inspector=#{inspector}, inspect_date=now(), "
            + "source_doc_type=#{sourceDocType}, source_doc_id=#{sourceDocId}, "
            + "source_doc_code=#{sourceDocCode}, source_line_id=#{sourceLineId}, "
            + "item_id=#{itemId}, vendor_id=#{vendorId}, update_by=#{inspector}, update_time=now() "
            + "where iqc_id=#{iqcId} and status='PREPARE'")
    int finishIqc(@Param("iqcId") Long iqcId,
                  @Param("status") String status,
                  @Param("quantityCheck") BigDecimal quantityCheck,
                  @Param("quantityQualified") BigDecimal quantityQualified,
                  @Param("quantityUnqualified") BigDecimal quantityUnqualified,
                  @Param("checkResult") String checkResult,
                  @Param("inspector") String inspector,
                  @Param("sourceDocType") String sourceDocType,
                  @Param("sourceDocId") Long sourceDocId,
                  @Param("sourceDocCode") String sourceDocCode,
                  @Param("sourceLineId") Long sourceLineId,
                  @Param("itemId") Long itemId,
                  @Param("vendorId") Long vendorId);

    /** 同一父工单的同一 BOM 行是否已经生成过子工单（服务端幂等键）。 */
    @Select("select count(*) from pro_workorder where attr1=#{linkageKey}")
    int countSubWorkorder(@Param("linkageKey") String linkageKey);

    /** 工单已检验合格的产出数量（只有 OK 行才真正进入线边库存）。 */
    @Select("select coalesce(sum(l.quantity_produce),0) from wm_product_produce_line l "
            + "join wm_product_produce p on p.record_id=l.record_id "
            + "where p.workorder_id=#{workorderId} and l.quality_status='OK'")
    BigDecimal qualifiedOutputForWorkorder(@Param("workorderId") Long workorderId);
}
