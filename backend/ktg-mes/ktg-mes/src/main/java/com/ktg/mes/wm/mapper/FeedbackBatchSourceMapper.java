package com.ktg.mes.wm.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 报工批次范围的可信来源。仅已执行领料及直接子工单已放行合格产品可以被消耗。 */
public interface FeedbackBatchSourceMapper {

    @Select("select distinct d.batch_code from wm_issue_detail d "
            + "join wm_issue_header h on h.issue_id=d.issue_id "
            + "join wm_issue_line l on l.issue_id=h.issue_id and l.line_id=d.line_id and l.item_id=d.item_id "
            + "where h.workorder_id=#{workorderId} and h.status='FINISHED' "
            + "and d.item_id=#{itemId} and d.quantity>0 and d.batch_code is not null and d.batch_code<>'' "
            + "union "
            + "select distinct l.batch_code from wm_product_produce_line l "
            + "join wm_product_produce p on p.record_id=l.record_id "
            + "join pro_workorder child on child.workorder_id=p.workorder_id "
            + "where child.parent_id=#{workorderId} and child.product_id=#{itemId} "
            + "and l.item_id=child.product_id and p.status='FINISHED' and l.quality_status='OK' "
            + "and l.quantity_produce>0 and l.batch_code is not null and l.batch_code<>''")
    List<String> selectAllowedBatchCodes(@Param("workorderId") Long workorderId,
                                        @Param("itemId") Long itemId);
}
