package com.ktg.mes.wm.service.impl;

import java.util.List;
import java.util.regex.Pattern;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.BussinessException;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.md.domain.MdItem;
import com.ktg.mes.md.domain.MdItemBatchConfig;
import com.ktg.mes.md.service.IMdItemBatchConfigService;
import com.ktg.mes.md.service.IMdItemService;
import com.ktg.mes.pro.service.IProWorkorderLineService;
import com.ktg.mes.wm.utils.WmBarCodeUtil;
import com.ktg.system.service.CodeSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ktg.mes.wm.mapper.WmBatchMapper;
import com.ktg.mes.wm.domain.WmBatch;
import com.ktg.mes.wm.service.IWmBatchService;

/**
 * 批次记录Service业务层处理
 * 
 * @author yinjinlu
 * @date 2025-02-24
 */
@Service
public class WmBatchServiceImpl implements IWmBatchService 
{
    private static final String PILOT_SCOPE_PREFIX = "PILOT-";
    private static final Pattern PILOT_SCOPE_PATTERN = Pattern.compile("PILOT-[A-Z0-9]+");

    @Autowired
    private WmBatchMapper wmBatchMapper;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Autowired
    private IMdItemService mdItemService;

    @Autowired
    private WmBarCodeUtil barcodeUtil;

    @Autowired
    private IMdItemBatchConfigService mdItemBatchConfigService;

    @Autowired
    private IProWorkorderLineService proWorkorderLineService;

    /**
     * 查询批次记录
     * 
     * @param batchId 批次记录主键
     * @return 批次记录
     */
    @Override
    public WmBatch selectWmBatchByBatchId(Long batchId)
    {
        return wmBatchMapper.selectWmBatchByBatchId(batchId);
    }

    /**
     * 查询批次记录列表
     * 
     * @param wmBatch 批次记录
     * @return 批次记录
     */
    @Override
    public List<WmBatch> selectWmBatchList(WmBatch wmBatch)
    {
        return wmBatchMapper.selectWmBatchList(wmBatch);
    }

    @Override
    public List<WmBatch> selectForwardBatchList(String batchCode) {
        return wmBatchMapper.selectForwardBatchList(batchCode);
    }

    @Override
    public List<WmBatch> selectBackwardBatchList(String batchCode) {
        return wmBatchMapper.selectBackwardBatchList(batchCode);
    }

    /**
     * 新增批次记录
     * 
     * @param wmBatch 批次记录
     * @return 结果
     */
    @Override
    public int insertWmBatch(WmBatch wmBatch)
    {
        // 有工单上下文时必须在建批次前确定并校验工单行，禁止静默写 NULL。
        resolveWorkorderLine(wmBatch);
        // 批次号一律由服务端生成，忽略前端传入的 batchCode；前端批次表单仅作只读展示。
        if (wmBatch.getBatchSource() == null) {
            wmBatch.setBatchSource(resolveDefaultBatchSource(wmBatch));
        }
        wmBatch.setBatchCode(generateBatchCode(wmBatch));
        wmBatch.setCreateTime(DateUtils.getNowDate());
        return wmBatchMapper.insertWmBatch(wmBatch);
    }

    /**
     * 修改批次记录
     * 
     * @param wmBatch 批次记录
     * @return 结果
     */
    @Override
    public int updateWmBatch(WmBatch wmBatch)
    {
        wmBatch.setUpdateTime(DateUtils.getNowDate());
        return wmBatchMapper.updateWmBatch(wmBatch);
    }

    /**
     * 批量删除批次记录
     * 
     * @param batchIds 需要删除的批次记录主键
     * @return 结果
     */
    @Override
    public int deleteWmBatchByBatchIds(Long[] batchIds)
    {
        return wmBatchMapper.deleteWmBatchByBatchIds(batchIds);
    }

    /**
     * 删除批次记录信息
     * 
     * @param batchId 批次记录主键
     * @return 结果
     */
    @Override
    public int deleteWmBatchByBatchId(Long batchId)
    {
        return wmBatchMapper.deleteWmBatchByBatchId(batchId);
    }


    /**
     * 获取批次号
     * 根据传入的参数获取批次号
     * 如果根据参数可以查询到批次号，则返回批次号，否则生成新的批次号
     * @param wmBatch
     * @return
     */
    @Override
    public WmBatch getOrGenerateBatchCode(WmBatch wmBatch) {
        // 仅显式试产命名空间启用本次批次隔离；必须在配置清空 poCode/lotNumber 前读取。
        String pilotScope = readPilotScope(wmBatch);

        //1.检查物料ID/物料编码参数是否为空
        if(wmBatch.getItemId() == null && wmBatch.getItemCode() == null){
            throw new IllegalArgumentException("物料ID/物料编码不能同时为空!");
        }else{
            //检查是否批次管理
            MdItem item = mdItemService.selectMdItemById(wmBatch.getItemId());
            if(UserConstants.YES.equals(item.getBatchFlag())){
                MdItemBatchConfig config = mdItemBatchConfigService.getMdItemBatchConfigByItemId(wmBatch.getItemId());
                if(config == null){
                    throw new IllegalArgumentException("物料"+wmBatch.getItemCode()+"未配置批次管理!");
                }

                //检查传入的参数是否符合配置
                if(UserConstants.YES.equals(config.getProduceDateFlag())){
                    if(wmBatch.getProduceDate() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要生产日期参数!");
                    }
                }else{
                    wmBatch.setProduceDate(null); //不需要则无论是否传入此参数都直接置空
                }

                if(UserConstants.YES.equals(config.getRecptDateFlag())){
                    if(wmBatch.getRecptDate() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要入库日期参数!");
                    }
                }else{
                    wmBatch.setRecptDate(null);
                }

                if(UserConstants.YES.equals(config.getExpireDateFlag())){
                    if(wmBatch.getExpireDate() == null){
                        throw new IllegalArgumentException("当前物料批生成次号，需要过期日期参数!");
                    }
                }else{
                    wmBatch.setExpireDate(null);
                }

                if(UserConstants.YES.equals(config.getVendorFlag())){
                    if(wmBatch.getVendorId() == null && wmBatch.getVendorCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要供应商参数!");
                    }
                }else{
                    wmBatch.setVendorId(null);
                    wmBatch.setVendorCode(null);
                }

                if(UserConstants.YES.equals(config.getClientFlag())){
                    if(wmBatch.getClientId() == null && wmBatch.getClientCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要客户参数!");
                    }
                }else{
                    wmBatch.setClientId(null);
                    wmBatch.setClientCode(null);
                }

                if(UserConstants.YES.equals(config.getPoCodeFlag())){
                    if(wmBatch.getPoCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要订单号参数!");
                    }
                }else{
                    wmBatch.setPoCode(null);
                }

                if(UserConstants.YES.equals(config.getCoCodeFlag())){
                    if(wmBatch.getCoCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要销售订单号参数!");
                    }
                }else{
                    wmBatch.setCoCode(null);
                }

                if(UserConstants.YES.equals(config.getWorkorderFlag())){
                    if(wmBatch.getWorkorderId() == null && wmBatch.getWorkorderCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要生产工单号参数!");
                    }
                }else{
                    // 该物料的批次维度不含工单：工单号与工单行必须同时清空，
                    // 否则会留下「有工单行、无工单」的上下文，被后续归属校验判为脏数据。
                    wmBatch.setWorkorderId(null);
                    wmBatch.setWorkorderCode(null);
                    wmBatch.setWorkorderLineId(null);
                }

                if(UserConstants.YES.equals(config.getTaskFlag())){
                    if(wmBatch.getTaskId() == null && wmBatch.getTaskCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要生产任务参数!");
                    }
                }else{
                    wmBatch.setTaskId(null);
                    wmBatch.setTaskCode(null);
                }

                if(UserConstants.YES.equals(config.getWorkstationFlag())){
                    if(wmBatch.getWorkstationId() == null && wmBatch.getWorkstationCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要工作站参数!");
                    }
                }else{
                    wmBatch.setWorkstationId(null);
                    wmBatch.setWorkstationCode(null);
                }

                if(UserConstants.YES.equals(config.getToolFlag())){
                    if(wmBatch.getToolId() == null && wmBatch.getToolCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要工具参数!");
                    }
                }else{
                    wmBatch.setToolId(null);
                    wmBatch.setToolCode(null);
                }

                if(UserConstants.YES.equals(config.getMoldFlag())){
                    if(wmBatch.getMoldId() == null && wmBatch.getMoldCode() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要模具参数!");
                    }
                }else{
                    wmBatch.setMoldId(null);
                    wmBatch.setMoldCode(null);
                }

                if(UserConstants.YES.equals(config.getLotNumberFlag())){
                    if(wmBatch.getLotNumber() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要生产批号参数!");
                    }
                }else{
                    wmBatch.setLotNumber(null);
                }

                if(UserConstants.YES.equals(config.getQualityStatusFlag())){
                    if(wmBatch.getQualityStatus() == null){
                        throw new IllegalArgumentException("当前物料生成批次号，需要质量状态参数!");
                    }
                }else{
                    wmBatch.setQualityStatus(null);
                }
            } else {
                //未启用批次管理则直接返回NULL
                return null;
            }
        }

        // 经过批次属性过滤后，工单上下文可能已被置空；此时有 line 无工单属于脏数据，必须报错。
        resolveWorkorderLine(wmBatch);

        // 原有必填维度校验全部通过后再保留命名空间，普通业务仍完全服从物料批次配置。
        if (pilotScope != null) {
            wmBatch.setLotNumber(pilotScope);
        }

        //2.根据参数查询现有的批次号
        WmBatch batch = wmBatchMapper.getBatchCodeByParams(wmBatch);

        //3.如果查询到批次号，则返回批次号，否则生成新的批次号
        if(batch != null){
            return batch;
        }

        //4.生成新的批次号（服务端取号，忽略任何外部传入的批次号）
        if (wmBatch.getBatchSource() == null) {
            wmBatch.setBatchSource(resolveDefaultBatchSource(wmBatch));
        }
        String batchCode = generateBatchCode(wmBatch);
        wmBatch.setBatchCode(batchCode);
        wmBatchMapper.insertWmBatch(wmBatch);
        barcodeUtil.generateBarCode(UserConstants.BARCODE_TYPE_BATCH,wmBatch.getBatchId(),wmBatch.getBatchCode(), wmBatch.getItemName()); //为每个批次号生成一个码
        return wmBatch;
    }

    private String readPilotScope(WmBatch batch) {
        String scope = null;
        for (String candidate : new String[]{batch.getPoCode(), batch.getLotNumber()}) {
            if (candidate == null || !candidate.startsWith(PILOT_SCOPE_PREFIX)) continue;
            // 采购订单号字段最多 64 字符；两条来源链采用同一格式和长度约束。
            if (candidate.length() > 64 || !PILOT_SCOPE_PATTERN.matcher(candidate).matches()) {
                throw new IllegalArgumentException("试产批次标识必须为 PILOT- 后接大写字母或数字，且总长不超过64字符");
            }
            if (scope != null && !scope.equals(candidate)) {
                throw new IllegalArgumentException("采购订单号与生产批号中的试产批次标识不一致");
            }
            scope = candidate;
        }
        return scope;
    }

    @Override
    public WmBatch checkBatchCodeByBatchAndProperties(WmBatch wmBatch) {
        //未启用批次管理则直接返回NULL
        MdItem item = mdItemService.selectMdItemById(wmBatch.getItemId());
        if(UserConstants.NO.equals(item.getBatchFlag())){
            return null;
        }

        //1.批次号和物料不能为空
        if(wmBatch.getBatchCode() == null && wmBatch.getBatchId() == null){
            throw new IllegalArgumentException("批次号不能为空!");
        }
        if(wmBatch.getItemId() == null && wmBatch.getItemCode() == null){
            throw new IllegalArgumentException("物资不能为空!");
        }

        //2.查询批次号与对应的树形是否匹配
        WmBatch batch = wmBatchMapper.checkBatchCodeByBatchAndProperties(wmBatch);

        //3.如果查询到批次号，则返回批次号，否则生成新的批次号
        if(batch != null){
            return batch;
        }

        //4.只通过批次号匹配
        WmBatch param = new WmBatch();
        param.setBatchId(wmBatch.getBatchId());
        param.setBatchCode(wmBatch.getBatchCode());
        WmBatch oldBatch = wmBatchMapper.checkBatchCodeByBatchAndProperties(param);

        //5.更新除了batchId,batchCode之外的其他属性
        oldBatch.setBatchId(null);
        oldBatch.setBatchCode(null);
        oldBatch.updateFileds(wmBatch);


        // 复检等老链路若带工单上下文，同样必须能确定工单行
        resolveWorkorderLine(oldBatch);

        //4.生成新的批次号（服务端取号，忽略任何外部传入的批次号）
        if (oldBatch.getBatchSource() == null) {
            oldBatch.setBatchSource(resolveDefaultBatchSource(oldBatch));
        }
        String batchCode = generateBatchCode(oldBatch);
        oldBatch.setBatchCode(batchCode);
        wmBatchMapper.insertWmBatch(oldBatch);

        return oldBatch;
    }

    /**
     * 批次写入前统一确定 workorder_line_id。
     *
     * <p>规则与 IProWorkorderLineService#resolveLineId 一致：</p>
     * <ul>
     *   <li>有工单：显式 lineId 校验归属；未显式传则按“同工单 + 同物料 + 未关闭”唯一匹配，
     *       匹配不到/匹配多条直接抛业务异常，不允许静默写 NULL；</li>
     *   <li>无工单：允许 workorder_line_id 为空（采购/外协等无工单批次）；
     *       若出现“有 line 无工单”则说明上游传错，直接拒绝。</li>
     * </ul>
     */
    private void resolveWorkorderLine(WmBatch batch)
    {
        if (batch == null) {
            return;
        }
        if (batch.getWorkorderId() == null) {
            if (batch.getWorkorderLineId() != null) {
                throw new BussinessException("批次指定了工单行[" + batch.getWorkorderLineId()
                        + "]但未指定工单，无法校验行归属，已拒绝写入");
            }
            return;
        }
        batch.setWorkorderLineId(proWorkorderLineService.resolveLineId(
                batch.getWorkorderId(), batch.getWorkorderLineId(), batch.getItemId()));
    }

    /**
     * 未显式声明批次来源时推断默认来源：带工单的是工单产出，其余按采购来料处理。
     */
    private String resolveDefaultBatchSource(WmBatch batch) {
        if (batch.getWorkorderId() != null || batch.getWorkorderCode() != null) {
            return "WORKORDER";
        }
        return "PURCHASE";
    }

    /**
     * 批次号前缀按来源选择：工单产出 pcgd、采购来料 pccg、外协入库 pcws。
     *
     * <p>历史兼容：未声明来源且没有工单的老链路按 pccg 兜底。</p>
     */
    private String resolveBatchRuleCode(WmBatch batch) {
        if (batch.getWorkorderId() != null || batch.getWorkorderCode() != null
                || "WORKORDER".equalsIgnoreCase(batch.getBatchSource())) {
            return UserConstants.CODE_RULE_BATCH_WORKORDER;
        }
        if ("OUTSOURCE".equalsIgnoreCase(batch.getBatchSource())) {
            return UserConstants.CODE_RULE_BATCH_OUTSOURCE;
        }
        return UserConstants.CODE_RULE_BATCH_PURCHASE;
    }

    /**
     * 工单产出批次走链号（pc + 订单日期流水，如 pc2026112100004），
     * 采购/外协批次不在订单链上，仍按各自规则独立取号。
     */
    private String generateBatchCode(WmBatch batch) {
        String ruleCode = resolveBatchRuleCode(batch);
        String workorderCode = batch.getWorkorderCode();
        if (UserConstants.CODE_RULE_BATCH_WORKORDER.equals(ruleCode)
                && workorderCode != null && !workorderCode.trim().isEmpty()) {
            return codeSequenceService.generateChainCode(ruleCode, workorderCode);
        }
        return codeSequenceService.generateCode(ruleCode);
    }

    @Override
    public java.util.Map<String,Object> traceBatch(String batchCode) {
        java.util.Map<String,Object> result = new java.util.LinkedHashMap<>();
        if (batchCode == null || batchCode.trim().isEmpty()) {
            result.put("error", "批次号不能为空");
            return result;
        }
        String code = batchCode.trim();
        result.put("batchCode", code);
        result.put("receipts", wmBatchMapper.traceReceipts(code));
        result.put("issueLines", wmBatchMapper.traceIssueLines(code));
        result.put("transactions", wmBatchMapper.traceTransactions(code));
        result.put("sales", wmBatchMapper.traceSales(code));
        result.put("note", "同批次关联与来源键关联分别列出；销售单与工单无直接来源键，仅按批次并列展示");
        return result;
    }
}
