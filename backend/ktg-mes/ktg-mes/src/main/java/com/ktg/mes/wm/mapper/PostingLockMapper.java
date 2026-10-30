package com.ktg.mes.wm.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 现有单据过账的数据库锁；必须在调用者事务内使用，锁持有到整个业务提交。 */
public interface PostingLockMapper {
    @Select("select status from wm_product_recpt where recpt_id=#{id} for update")
    String productRecpt(Long id);
    @Select("select status from wm_product_sales where sales_id=#{id} for update")
    String productSales(Long id);
    @Select("select status from wm_item_recpt where recpt_id=#{id} for update")
    String itemRecpt(Long id);
    @Select("select status from wm_issue_header where issue_id=#{id} for update")
    String issue(Long id);
    @Select("select status from wm_rt_issue where rt_id=#{id} for update")
    String rtIssue(Long id);
    @Select("select status from wm_rt_vendor where rt_id=#{id} for update")
    String rtVendor(Long id);
    @Select("select rt_id from wm_rt_vendor_line where line_id=#{id} for update")
    Long rtVendorIdForLine(Long id);
    @Select("select rt_id from wm_rt_vendor_detail where detail_id=#{id} for update")
    Long rtVendorIdForDetail(Long id);
    @Select("select status from qc_rqc where rqc_id=#{id} for update")
    String rqc(Long id);
    @Select("select source_doc_type from qc_rqc where rqc_id=#{id} for update")
    String rqcSourceType(Long id);
    @Select("select source_doc_id from qc_rqc where rqc_id=#{id} and source_doc_type='RT_ISSUE' for update")
    Long rqcRtIssueSource(Long id);
    @Select("select rt_id from wm_rt_issue_line where line_id=#{id} for update")
    Long rtIssueIdForLine(Long id);
    @Select("select rt_id from wm_rt_issue_detail where detail_id=#{id} for update")
    Long rtIssueIdForDetail(Long id);
    /** 退料行是否已存在未取消的检验单（存在则冻结物料/批次/数量）。 */
    @Select("select rqc_id from qc_rqc where source_doc_type='RT_ISSUE' and source_line_id=#{lineId} "
            + "and status<>'CANCELED' order by rqc_id limit 1")
    Long rqcIdForRtIssueLine(@Param("lineId") Long lineId);
    @Select("select issue_id from wm_issue_detail where detail_id=#{id} for update")
    Long issueIdForDetail(Long id);
    @Select("select issue_id from wm_issue_line where line_id=#{id} for update")
    Long issueIdForLine(Long id);
    @Select("select status from pro_feedback where record_id=#{id} for update")
    String feedback(Long id);
    @Select("select status from qc_ipqc where ipqc_id=#{id} for update")
    String ipqc(Long id);
    @Select("select status from wm_product_produce where record_id=#{id} for update")
    String produce(Long id);
    @Select("select status from pro_workorder where workorder_id=#{id} for update")
    String workorder(Long id);
    @Select("select status from pro_task where task_id=#{id} for update")
    String task(Long id);
    @Select("select status from qc_iqc where iqc_id=#{id} for update")
    String iqc(Long id);
    @Select("select status from wm_arrival_notice where notice_id=#{id} for update")
    String arrivalNotice(Long id);
    // 固定顺序串行化库存过账，包括尚不存在的库存行，避免跨实例创建重复余额行。
    @Select("select warehouse_id from wm_warehouse order by warehouse_id for update")
    List<Long> inventory();
}
