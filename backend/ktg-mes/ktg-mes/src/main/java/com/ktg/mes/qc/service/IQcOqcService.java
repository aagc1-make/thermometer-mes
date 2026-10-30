package com.ktg.mes.qc.service;

import java.util.List;
import com.ktg.mes.qc.domain.QcOqc;

/**
 * 出货检验单Service接口
 * 
 * @author yinjinlu
 * @date 2022-08-31
 */
public interface IQcOqcService 
{
    /**
     * 查询出货检验单
     * 
     * @param oqcId 出货检验单主键
     * @return 出货检验单
     */
    public QcOqc selectQcOqcByOqcId(Long oqcId);

    /**
     * 查询出货检验单列表
     * 
     * @param qcOqc 出货检验单
     * @return 出货检验单集合
     */
    public List<QcOqc> selectQcOqcList(QcOqc qcOqc);


    /**
     * 检查出货检验单号是否唯一
     * @param qcOqc
     * @return
     */
    public String checkOqcCodeUnique(QcOqc qcOqc);

    /**
     * 新增出货检验单
     * 
     * @param qcOqc 出货检验单
     * @return 结果
     */
    public int insertQcOqc(QcOqc qcOqc);

    /**
     * 修改出货检验单
     * 
     * @param qcOqc 出货检验单
     * @return 结果
     */
    public int updateQcOqc(QcOqc qcOqc);

    /**
     * 带旧状态条件的"完成"操作（阶段1复审修复③）
     *
     * 用一条 UPDATE ... WHERE status &lt;&gt; 'FINISHED' 决定唯一成功者，
     * 并发提交时只有一个请求能拿到 1 行受影响，其余返回 0。
     *
     * @param oqcId 出货检验单主键
     * @return 受影响行数（1=本次完成成功，0=已被其他请求完成）
     */
    public int finishOqcIfPending(Long oqcId);

    /**
     * 出货检验完成的统一事务编排（PC / 移动端共用）
     *
     * 内含：并发条件更新、销售行质量回写、销售主单状态汇总、检验单更新。
     * 失败抛 ServiceException 触发整体回滚，不会留下"已完成但未回写"的数据。
     *
     * @param qcOqc 出货检验单（至少含 oqcId、sourceDocId、sourceLineId、checkResult、oqcCode）
     */
    public void completeOqc(QcOqc qcOqc);

    /**
     * 根据缺陷记录更新头上的缺陷数量和缺陷比率
     * @param oqcId
     * @return
     */
    public int updateCrMajMinQuaAndRate(Long oqcId);

    /**
     * 批量删除出货检验单
     * 
     * @param oqcIds 需要删除的出货检验单主键集合
     * @return 结果
     */
    public int deleteQcOqcByOqcIds(Long[] oqcIds);

    /**
     * 删除出货检验单信息
     * 
     * @param oqcId 出货检验单主键
     * @return 结果
     */
    public int deleteQcOqcByOqcId(Long oqcId);
}
