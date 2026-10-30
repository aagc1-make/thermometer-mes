package com.ktg.mes.qc.controller;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.collection.CollUtil;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.bean.BeanUtils;
import com.ktg.mes.qc.domain.*;
import com.ktg.mes.qc.service.IQcRqcLineService;
import com.ktg.mes.qc.service.impl.QcRqcItemLineService;
import com.ktg.mes.qc.service.IQcTemplateIndexService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.service.*;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ktg.common.annotation.Log;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;
import com.ktg.mes.qc.service.IQcRqcService;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.common.core.page.TableDataInfo;

/**
 * 退料检验单Controller
 * 
 * @author yinjinlu
 * @date 2025-03-06
 */
@RestController
@RequestMapping("/mes/qc/rqc")
public class QcRqcController extends BaseController
{
    @Autowired
    private PostingLockMapper postingLockMapper;
    @Autowired
    private IQcRqcService qcRqcService;

    @Autowired
    private IQcRqcLineService qcRqcLineService;
    @Autowired
    private QcRqcItemLineService rqcItemLines;

    @Autowired
    private IQcTemplateProductService qcTemplateProductService;

    @Autowired
    private IWmRtIssueLineService wmRtIssueLineService;

    @Autowired
    private IWmRtIssueService wmRtIssueService;

    @Autowired
    private IWmRtSalesLineService wmRtSalesLineService;

    @Autowired
    private IWmRtSalesService wmRtSalesService;

    @Autowired
    private IWmBatchService wmBatchService;

    @Autowired
    private IQcTemplateIndexService qcTemplateIndexService;

    /**
     * 查询退料检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:list')")
    @GetMapping("/list")
    public TableDataInfo list(QcRqc qcRqc)
    {
        startPage();
        List<QcRqc> list = qcRqcService.selectQcRqcList(qcRqc);
        return getDataTable(list);
    }

    /**
     * 导出退料检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:export')")
    @Log(title = "退料检验单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QcRqc qcRqc)
    {
        List<QcRqc> list = qcRqcService.selectQcRqcList(qcRqc);
        ExcelUtil<QcRqc> util = new ExcelUtil<QcRqc>(QcRqc.class);
        util.exportExcel(response, list, "退料检验单数据");
    }

    /**
     * 获取退料检验单详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:query')")
    @GetMapping(value = "/{rqcId}")
    public AjaxResult getInfo(@PathVariable("rqcId") Long rqcId)
    {
        return AjaxResult.success(qcRqcService.selectQcRqcByRqcId(rqcId));
    }

    /**
     * 新增退料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:add')")
    @Log(title = "退料检验单", businessType = BusinessType.INSERT)
    @Transactional
    @PostMapping
    public AjaxResult add(@RequestBody QcRqc qcRqc)
    {
        if(UserConstants.NOT_UNIQUE.equals(qcRqcService.checkCodeUnique(qcRqc))){
            return AjaxResult.error("单据编号已存在！");
        }

        QcTemplateProduct param = new QcTemplateProduct();
        param.setItemId(qcRqc.getItemId());
        List<QcTemplateProduct> templates = qcTemplateProductService.selectQcTemplateProductList(param);
        if(CollUtil.isNotEmpty(templates)){
            qcRqc.setTemplateId(templates.get(0).getTemplateId());
        }else{
            return AjaxResult.error("当前物资未配置检测模板！");
        }

        //检测数量效验
        if((qcRqc.getQuantityUnqualified() == null || qcRqc.getQuantityUnqualified().compareTo(BigDecimal.ZERO) <= 0)&&
                (qcRqc.getQuantityQualified() == null || qcRqc.getQuantityQualified().compareTo(BigDecimal.ZERO) <= 0)){
            return AjaxResult.error("请填写合格品/不合格品数量！");
        }

        if(qcRqc.getQuantityCheck().compareTo(qcRqc.getQuantityQualified().add(qcRqc.getQuantityUnqualified()))!=0){
            return AjaxResult.error("检测数量与合格品/不合格品总和数量不匹配！");
        }

        String conclusionError = checkConclusionConsistency(qcRqc);
        if (conclusionError != null) {
            return AjaxResult.error(conclusionError);
        }

        qcRqcService.insertQcRqc(qcRqc);

        generateLine(qcRqc);

        return AjaxResult.success(qcRqc.getRqcId());
    }

    /**
     * 修改退料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:edit')")
    @Log(title = "退料检验单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping
    public AjaxResult edit(@RequestBody QcRqc qcRqc)
    {
        if (qcRqc.getRqcId() == null) return AjaxResult.error("请先保存退料检验单");
        String status = postingLockMapper.rqc(qcRqc.getRqcId());
        if (status == null || UserConstants.ORDER_STATUS_FINISHED.equals(status)
                || UserConstants.ORDER_STATUS_CANCELED.equals(status)) {
            return AjaxResult.error("退料检验单不存在、已完成或已取消");
        }
        String sourceType = postingLockMapper.rqcSourceType(qcRqc.getRqcId());
        if (UserConstants.ORDER_STATUS_FINISHED.equals(qcRqc.getStatus())) {
            Long sourceId = postingLockMapper.rqcRtIssueSource(qcRqc.getRqcId());
            if (UserConstants.SOURCE_DOC_TYPE_RTISSUE.equals(sourceType) && sourceId == null) {
                return AjaxResult.error("退料检验单缺少来源退料单");
            }
            if (!UserConstants.SOURCE_DOC_TYPE_RTISSUE.equals(sourceType)
                    && UserConstants.SOURCE_DOC_TYPE_RTISSUE.equals(qcRqc.getSourceDocType())) {
                return AjaxResult.error("退料检验的来源类型不能修改");
            }
            if (sourceId != null) {
                if (!UserConstants.RTISSUE_STATUS_UNCHECK.equals(postingLockMapper.rtIssue(sourceId))) {
                    return AjaxResult.error("来源退料单不在待检状态");
                }
                QcRqc db = qcRqcService.selectQcRqcByRqcId(qcRqc.getRqcId());
                if (db == null || !Objects.equals(sourceId, db.getSourceDocId())
                        || (qcRqc.getSourceDocId() != null && !Objects.equals(sourceId, qcRqc.getSourceDocId()))
                        || (qcRqc.getSourceLineId() != null && !Objects.equals(db.getSourceLineId(), qcRqc.getSourceLineId()))
                        || (qcRqc.getSourceDocType() != null && !Objects.equals(db.getSourceDocType(), qcRqc.getSourceDocType()))
                        || (qcRqc.getItemId() != null && !Objects.equals(db.getItemId(), qcRqc.getItemId()))) {
                    return AjaxResult.error("退料检验的来源或物料不能修改");
                }
                WmRtIssueLine source = wmRtIssueLineService.selectWmRtIssueLineByLineId(db.getSourceLineId());
                if (source == null || !Objects.equals(sourceId, source.getRtId())
                        || !Objects.equals(db.getItemId(), source.getItemId())
                        || !UserConstants.QUALITY_STATUS_NOTTEST.equals(source.getQualityStatus())) {
                    return AjaxResult.error("来源退料行不存在、归属不符或已完成检验");
                }
                if (qcRqc.getQuantityCheck() == null || qcRqc.getQuantityQualified() == null
                        || qcRqc.getQuantityUnqualified() == null
                        || qcRqc.getQuantityCheck().signum() <= 0
                        || qcRqc.getQuantityQualified().signum() < 0 || qcRqc.getQuantityUnqualified().signum() < 0
                        || source.getQuantityRt() == null || qcRqc.getQuantityCheck().compareTo(source.getQuantityRt()) != 0) {
                    return AjaxResult.error("检测数量须等于来源退料数量，且合格和不合格数量不得为负");
                }
                qcRqc.setSourceDocId(sourceId); qcRqc.setSourceLineId(db.getSourceLineId());
                qcRqc.setSourceDocType(db.getSourceDocType()); qcRqc.setSourceDocCode(db.getSourceDocCode());
                qcRqc.setItemId(db.getItemId());
            }
        } else if (UserConstants.SOURCE_DOC_TYPE_RTISSUE.equals(sourceType)) {
            QcRqc db = qcRqcService.selectQcRqcByRqcId(qcRqc.getRqcId());
            if (db == null || (qcRqc.getSourceDocId() != null && !Objects.equals(db.getSourceDocId(), qcRqc.getSourceDocId()))
                    || (qcRqc.getSourceLineId() != null && !Objects.equals(db.getSourceLineId(), qcRqc.getSourceLineId()))
                    || (qcRqc.getSourceDocType() != null && !Objects.equals(sourceType, qcRqc.getSourceDocType()))
                    || (qcRqc.getItemId() != null && !Objects.equals(db.getItemId(), qcRqc.getItemId()))) {
                return AjaxResult.error("退料检验的来源或物料不能修改");
            }
            qcRqc.setSourceDocId(db.getSourceDocId()); qcRqc.setSourceLineId(db.getSourceLineId());
            qcRqc.setSourceDocType(sourceType); qcRqc.setSourceDocCode(db.getSourceDocCode());
            qcRqc.setItemId(db.getItemId());
        }
        if(UserConstants.NOT_UNIQUE.equals(qcRqcService.checkCodeUnique(qcRqc))){
            return AjaxResult.error("单据编号已存在！");
        }

        QcTemplateProduct param = new QcTemplateProduct();
        param.setItemId(qcRqc.getItemId());
        List<QcTemplateProduct> templates = qcTemplateProductService.selectQcTemplateProductList(param);
        if(CollUtil.isNotEmpty(templates)){
            qcRqc.setTemplateId(templates.get(0).getTemplateId());
        }else{
            return AjaxResult.error("当前物资未配置检测模板！");
        }

        //检测数量效验
        if((qcRqc.getQuantityUnqualified() == null || qcRqc.getQuantityUnqualified().compareTo(BigDecimal.ZERO) <= 0)&&
                (qcRqc.getQuantityQualified() == null || qcRqc.getQuantityQualified().compareTo(BigDecimal.ZERO) <= 0)){
            return AjaxResult.error("请填写合格品/不合格品数量！");
        }

        if(qcRqc.getQuantityCheck().compareTo(qcRqc.getQuantityQualified().add(qcRqc.getQuantityUnqualified()))!=0){
            return AjaxResult.error("检测数量与合格品/不合格品总和数量不匹配！");
        }

        // 结论与数量一致性（完成时必须显式且一致），放在任何来源回写之前
        String conclusionError = checkConclusionConsistency(qcRqc);
        if (conclusionError != null) {
            return AjaxResult.error(conclusionError);
        }

        //完成状态时,处理来源单据的状态、数量、质量状态等数据
        if(UserConstants.ORDER_STATUS_FINISHED.equals(qcRqc.getStatus())){
            if(qcRqc.getSourceDocId() != null){
                //如果是生产退料单，则根据合格品数量、不合格品数量对来源单据行进行拆分
                if(UserConstants.SOURCE_DOC_TYPE_RTISSUE.equals(qcRqc.getSourceDocType())){
                    //1.获取生产退料单行
                    WmRtIssueLine sourceLine =  wmRtIssueLineService.selectWmRtIssueLineByLineId(qcRqc.getSourceLineId());

                    //2.质量状态不同时，判断行是否需要生产一个新的批次;方法是调用批次管理的checkBatchCodeByBatchAndProperties方法。
                    /**
                     * 为了系统逻辑简单起见（做减法），默认质量状态不参与批次规则；如此一来合格品和不良品的批次还是沿用原有批次，在库存层面不进行区分。实际操作时由相关人员在线下进行标记和区分（例如打标记，将物资存放在独立的库区等）
                    if(qcRqc.getQuantityUnqualified().compareTo(BigDecimal.ZERO) > 0){
                        WmBatch batch = new WmBatch();
                        batch.setBatchId(qcRqc.getBatchId());
                        batch.setBatchCode(qcRqc.getBatchCode());
                        batch.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                        batch = wmBatchService.checkBatchCodeByBatchAndProperties(batch);
                        WmRtIssueLine line = new WmRtIssueLine();
                        try {
                            BeanUtils.copyProperties(sourceLine, line);
                        } catch (Exception e) {
                            return AjaxResult.error("拆分行数据异常！");
                        }
                        line.setBatchId(batch.getBatchId());
                        line.setBatchCode(batch.getBatchCode());
                        line.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                        line.setQuantityRt(qcRqc.getQuantityUnqualified());
                        wmRtIssueLineService.insertInspectionSplit(line);
                    }
                     **/
                    if(qcRqc.getQuantityUnqualified().compareTo(BigDecimal.ZERO) == 0){
                        //直接更新生产退料单行的质量状态为合格
                        sourceLine.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
                    }else if(qcRqc.getQuantityQualified().compareTo(BigDecimal.ZERO) == 0){
                        //直接更新生产退料单行的质量状态为不合格
                        sourceLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                    } else{
                        //将生产退料单的待检测行拆分成两行，一个不合格品，一个合格品，批次一致
                        WmRtIssueLine line = new WmRtIssueLine();
                        try {
                            BeanUtils.copyBeanProp(line,sourceLine);
                        } catch (Exception e) {
                            return AjaxResult.error("拆分行数据异常！");
                        }
                        line.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                        line.setQuantityRt(qcRqc.getQuantityUnqualified());
                        wmRtIssueLineService.insertInspectionSplit(line);

                        sourceLine.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
                        sourceLine.setQuantityRt(qcRqc.getQuantityQualified());
                    }
                    wmRtIssueLineService.applyInspectionResult(sourceLine);

                    //3.判断生产退料单行上是否还有待检验的物资，如果没有，则将生产退料单的状态设置为待上架
                    WmRtIssueLine paramLine = new WmRtIssueLine();
                    paramLine.setRtId(qcRqc.getSourceDocId());
                    paramLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTTEST);
                    List<WmRtIssueLine> lines = wmRtIssueLineService.selectWmRtIssueLineList(paramLine);
                    if(CollUtil.isEmpty(lines)){
                        WmRtIssue issue = wmRtIssueService.selectWmRtIssueByRtId(qcRqc.getSourceDocId());
                        issue.setStatus(UserConstants.RTISSUE_STATUS_UNSTOCK);
                        wmRtIssueService.updateWmRtIssue(issue);
                    }
                } else if(UserConstants.SOURCE_DOC_TYPE_RTSALSE.equals(qcRqc.getSourceDocType())){
                    WmRtSalesLine sourceLine =  wmRtSalesLineService.selectWmRtSalesLineByLineId(qcRqc.getSourceLineId());
                    if(qcRqc.getQuantityUnqualified().compareTo(BigDecimal.ZERO) == 0){
                        //直接更新销售退料单行的质量状态为合格
                        sourceLine.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
                    }else if(qcRqc.getQuantityQualified().compareTo(BigDecimal.ZERO) == 0) {
                        //直接更新销售退料单行的质量状态为不合格
                        sourceLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                    }else{
                        WmRtSalesLine line = new WmRtSalesLine();
                        try {
                            BeanUtils.copyBeanProp(line,sourceLine);
                        } catch (Exception e) {
                            return AjaxResult.error("拆分行数据异常！");
                        }
                        line.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                        line.setQuantityRted(qcRqc.getQuantityUnqualified());
                        wmRtSalesLineService.insertWmRtSalesLine(line);

                        sourceLine.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
                        sourceLine.setQuantityRted(qcRqc.getQuantityQualified());
                        wmRtSalesLineService.updateWmRtSalesLine(sourceLine);
                    }

                    WmRtSalesLine paramLine = new WmRtSalesLine();
                    paramLine.setRtId(qcRqc.getSourceDocId());
                    paramLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTTEST);
                    List<WmRtSalesLine> lines = wmRtSalesLineService.selectWmRtSalesLineList(paramLine);
                    if(CollUtil.isEmpty(lines)){
                        WmRtSales sales = wmRtSalesService.selectWmRtSalesByRtId(qcRqc.getSourceDocId());
                        sales.setStatus(UserConstants.RT_SALES_STATUS_UNSTOCK);
                        wmRtSalesService.updateWmRtSales(sales);
                    }
                } else{
                    return AjaxResult.error("来源单据类型不正确！");
                }
            }
        }
        return toAjax(qcRqcService.updateQcRqc(qcRqc));
    }

    /**
     * 删除退料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:rqc:remove')")
    @Log(title = "退料检验单", businessType = BusinessType.DELETE)
    @Transactional
	@DeleteMapping("/{rqcIds}")
    public AjaxResult remove(@PathVariable Long[] rqcIds)
    {
        for (Long rqcId:rqcIds){
                QcRqc qcRqc = qcRqcService.selectQcRqcByRqcId(rqcId);
                if(!UserConstants.ORDER_STATUS_PREPARE.equals(qcRqc.getStatus())){
                    return AjaxResult.error("只能删除草稿状态的单据！");
                }
                qcRqcLineService.deleteQcRqcLineByRqcId(rqcId);
        }

        return toAjax(qcRqcService.deleteQcRqcByRqcIds(rqcIds));
    }

    /**
     * 根据头信息生成行信息
     * @param rqc
     */
    /**
     * 完成检验时校验结论与数量一致：合格量>0 必须 ACCEPT，合格量=0 必须 REJECT。
     * 混合结果（合格、不合格都>0）仍按 ACCEPT 处理，保持现有安全行为；未填结论时由必填校验拦截。
     */
    private String checkConclusionConsistency(QcRqc rqc) {
        if (rqc == null || !UserConstants.ORDER_STATUS_FINISHED.equals(rqc.getStatus())) {
            return null;
        }
        BigDecimal qualified = rqc.getQuantityQualified() == null ? BigDecimal.ZERO : rqc.getQuantityQualified();
        String result = rqc.getCheckResult();
        if (result == null || result.trim().isEmpty()) {
            return "请选择检验结论（合格品数量大于 0 应选“检验通过”）";
        }
        String expected = qualified.compareTo(BigDecimal.ZERO) > 0 ? "ACCEPT" : "REJECT";
        if (!expected.equalsIgnoreCase(result.trim())) {
            return "检验结论与数量不一致：合格品数量为 "
                    + qualified.stripTrailingZeros().toPlainString()
                    + "，应选择 " + ("ACCEPT".equals(expected) ? "检验通过" : "检验不通过")
                    + "；如确为相反结论，请调整合格/不合格数量";
        }
        return null;
    }

    private void generateLine(QcRqc rqc){
        rqcItemLines.generate(rqc);
    }
}
