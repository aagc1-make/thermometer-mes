package com.ktg.mes.wm.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ktg.mes.wm.domain.WmBatch;

/**
 * 批次记录Mapper接口
 * 
 * @author yinjinlu
 * @date 2025-02-24
 */
public interface WmBatchMapper 
{
    /**
     * 查询批次记录
     * 
     * @param batchId 批次记录主键
     * @return 批次记录
     */
    public WmBatch selectWmBatchByBatchId(Long batchId);

    /**
     * 根据批次号查询批次记录
     * @param batchCode
     * @return
     */
    public WmBatch selectWmBatchByBatchCode(String batchCode);

    /**
     * 查询批次记录列表
     * 
     * @param wmBatch 批次记录
     * @return 批次记录集合
     */
    public List<WmBatch> selectWmBatchList(WmBatch wmBatch);


    /**
     * 根据批次号查询，当前批次被哪些工单的哪些批次产品给消耗掉了
     * @param batchCode
     * @return
     */
    public List<WmBatch> selectForwardBatchList(String batchCode);


    /**
     * 根据批次号查询，当前批次的产品都用到了哪些批次的物资
     * @param batchCode
     * @return
     */
    public List<WmBatch> selectBackwardBatchList(String batchCode);

    /**
     * 新增批次记录
     * 
     * @param wmBatch 批次记录
     * @return 结果
     */
    public int insertWmBatch(WmBatch wmBatch);

    /**
     * 修改批次记录
     * 
     * @param wmBatch 批次记录
     * @return 结果
     */
    public int updateWmBatch(WmBatch wmBatch);

    /**
     * 删除批次记录
     * 
     * @param batchId 批次记录主键
     * @return 结果
     */
    public int deleteWmBatchByBatchId(Long batchId);

    /**
     * 批量删除批次记录
     * 
     * @param batchIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteWmBatchByBatchIds(Long[] batchIds);

    /**
     * 根据传入的参数信息，查询已有的批次
     * @param wmBatch
     * @return
     */
    public WmBatch getBatchCodeByParams(WmBatch wmBatch);

    /**
     * 根据传入的批次号，和某些属性，判断这些属性是否与当前批次匹配，如果匹配则返回当前批次，否则生成新的批次
     * 应用场景是：例如某个批次的物资，其质量状态发生变化，此时传入批次号和质量状态参数。如果批次规则中包含质量状态，则根据质量状态参数判断是否与当前批次匹配，如果匹配则返回当前批次，否则生成新的批次（其余属性不变）。
     * @param wmBatch
     * @return
     */
    public WmBatch checkBatchCodeByBatchAndProperties(WmBatch wmBatch);


    /* ===== 只读批次追溯（来源键关联，不做名称/时间推断）===== */
    @Select("select rl.batch_code batchCode, r.recpt_id recptId, r.recpt_code recptCode, r.status, "
          + "r.workorder_id workorderId, r.workorder_code workorderCode, rl.item_code itemCode, rl.item_name itemName, "
          + "rl.quantity_recived qty from wm_product_recpt_line rl "
          + "join wm_product_recpt r on r.recpt_id=rl.recpt_id where rl.batch_code=#{batchCode}")
    List<Map<String,Object>> traceReceipts(@Param("batchCode") String batchCode);

    @Select("select i.issue_id issueId, i.issue_code issueCode, i.workorder_id workorderId, i.workorder_code workorderCode, "
          + "d.item_code itemCode, d.item_name itemName, d.batch_code materialBatch, d.quantity qty "
          + "from wm_issue_header i join wm_issue_detail d on d.issue_id=i.issue_id "
          + "where i.workorder_code in (select r.workorder_code from wm_product_recpt_line rl "
          + "join wm_product_recpt r on r.recpt_id=rl.recpt_id where rl.batch_code=#{batchCode} and r.workorder_code is not null)")
    List<Map<String,Object>> traceIssueLines(@Param("batchCode") String batchCode);

    @Select("select t.source_doc_code sourceDocCode, t.source_doc_type sourceDocType, t.batch_code batchCode, "
          + "t.item_code itemCode, t.transaction_type transactionType, t.transaction_quantity qty, t.create_time createTime "
          + "from wm_transaction t where t.batch_code=#{batchCode} or t.source_doc_code in ("
          + "select i.issue_code from wm_issue_header i join wm_issue_detail d on d.issue_id=i.issue_id "
          + "where i.workorder_code in (select r.workorder_code from wm_product_recpt_line rl "
          + "join wm_product_recpt r on r.recpt_id=rl.recpt_id where rl.batch_code=#{batchCode} and r.workorder_code is not null)) "
          + "order by t.transaction_id")
    List<Map<String,Object>> traceTransactions(@Param("batchCode") String batchCode);

    @Select("select s.sales_id salesId, s.sales_code salesCode, s.status, d.batch_code batchCode, d.item_code itemCode, "
          + "d.quantity qty from wm_product_sales_detail d join wm_product_sales s on s.sales_id=d.sales_id "
          + "where d.batch_code=#{batchCode}")
    List<Map<String,Object>> traceSales(@Param("batchCode") String batchCode);
}
