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
    // 固定顺序串行化库存过账，包括尚不存在的库存行，避免跨实例创建重复余额行。
    @Select("select warehouse_id from wm_warehouse order by warehouse_id for update")
    List<Long> inventory();
}
