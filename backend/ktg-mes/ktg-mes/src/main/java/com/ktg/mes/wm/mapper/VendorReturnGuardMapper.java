package com.ktg.mes.wm.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

/** 采购退供来源与累计量校验。 */
public interface VendorReturnGuardMapper {
    /** 未入库退供：当前仍占用来源的草稿（未完成且未取消），用于保证同一 IQC 只有一个进行中的草稿。 */
    @Select("select rt_id from wm_rt_vendor where attr1=concat('AUTO:RTV:IQC:',#{iqcId}) "
            + "and status not in ('FINISHED','CANCELED') order by rt_id limit 1")
    Long openPreStockDraft(@Param("iqcId") Long iqcId);

    /** 未入库退供：同一 IQC 已执行（FINISHED）的累计退供量；excludeRtId 用于执行时排除自身。 */
    @Select("select coalesce(sum(l.quantity_rted),0) from wm_rt_vendor_line l join wm_rt_vendor h on h.rt_id=l.rt_id "
            + "where h.attr1=concat('AUTO:RTV:IQC:',#{iqcId}) and h.status='FINISHED' and h.rt_id<>#{excludeRtId}")
    BigDecimal finishedPreStockReturned(@Param("iqcId") Long iqcId, @Param("excludeRtId") Long excludeRtId);

    @Select("select attr2 from wm_rt_vendor where rt_id=#{id} for update")
    String lockMode(@Param("id") Long id);
    @Select("select attr1 from wm_rt_vendor where rt_id=#{id} for update")
    String lockSourceKey(@Param("id") Long id);
    @Select("select coalesce(sum(quantity),0) from wm_item_recpt_detail where recpt_id=#{recptId} "
            + "and line_id=#{lineId} and batch_id <=> #{batchId} and batch_code <=> #{batchCode}")
    BigDecimal receivedBatchQuantity(@Param("recptId") Long recptId, @Param("lineId") Long lineId,
                                     @Param("batchId") Long batchId, @Param("batchCode") String batchCode);
    @Select("select coalesce(sum(l.quantity_rted),0) from wm_rt_vendor_line l join wm_rt_vendor h on h.rt_id=l.rt_id "
            + "where h.attr2='STOCK' and h.status='FINISHED' and h.rt_id<>#{excludeId} "
            + "and l.attr2=cast(#{lineId} as char) and l.batch_id <=> #{batchId} and l.batch_code <=> #{batchCode}")
    BigDecimal finishedBatchReturned(@Param("lineId") Long lineId, @Param("batchId") Long batchId,
                                     @Param("batchCode") String batchCode, @Param("excludeId") Long excludeId);
    @Select("select rt_id from wm_rt_vendor where attr1=concat('SRC:RECEIPT:',#{recptId}) and attr2='STOCK' "
            + "and status not in ('FINISHED','CANCELED') order by rt_id limit 1")
    Long openStockDraft(@Param("recptId") Long recptId);
}
