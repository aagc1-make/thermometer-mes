package com.ktg.mes.qc.controller.mobile;

import cn.hutool.core.collection.CollUtil;
import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.qc.domain.*;
import com.ktg.mes.qc.service.*;
import com.ktg.mes.wm.domain.WmItemRecptLine;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 移动端来料检验单接口
 */
@RestController
@RequestMapping("/mobile/qc/iqc")
public class QCIqcMobController extends BaseController {

    @Autowired
    private IQcIqcService qcIqcService;

    @Autowired
    private AutoCodeUtil autoCodeUtil;

    @Autowired
    private IQcTemplateProductService qcTemplateProductService;

    @Autowired
    private IQcTemplateIndexService qcTemplateIndexService;

    @Autowired
    private IQcIqcLineService qcIqcLineService;

    @Autowired
    private IQcDefectRecordService qcDefectRecordService;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private com.ktg.mes.qc.service.impl.QcIqcCompletionService qcIqcCompletionService;


    /**
     * 查询来料检验单列表
     */
    @GetMapping("/list")
    public TableDataInfo list(QcIqc qcIqc)
    {
        startPage();
        List<QcIqc> list = qcIqcService.selectQcIqcList(qcIqc);
        return getDataTable(list);
    }


    /**
     * 获取来料检验单详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:query')")
    @GetMapping(value = "/{iqcId}")
    public AjaxResult getInfo(@PathVariable("iqcId") Long iqcId)
    {
        return AjaxResult.success(qcIqcService.selectQcIqcByIqcId(iqcId));
    }


    /**
     * 新增来料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:add')")
    @Log(title = "来料检验单", businessType = BusinessType.INSERT)
    @Transactional
    @PostMapping
    public AjaxResult add(@RequestBody QcIqc qcIqc)
    {
        //质量的质检记录编码自动生成
        String resultCode = autoCodeUtil.genSerialCode(UserConstants.QC_IQC_CODE,null);
        qcIqc.setIqcCode(resultCode);

        QcTemplateProduct param = new QcTemplateProduct();
        param.setItemId(qcIqc.getItemId());
        List<QcTemplateProduct> templates = qcTemplateProductService.selectQcTemplateProductList(param);
        if(CollUtil.isNotEmpty(templates)){
            qcIqc.setTemplateId(templates.get(0).getTemplateId());
        }else{
            return AjaxResult.error("当前产品未配置检测模板！");
        }
        qcIqc.setInspector(getUsername());
        qcIqc.setCreateBy(getUsername());
        qcIqcService.insertQcIqc(qcIqc);
        generateLine(qcIqc);
        return AjaxResult.success(qcIqc);
    }

    /**
     * 修改来料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:edit')")
    @Log(title = "来料检验单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping
    public AjaxResult edit(@RequestBody QcIqc qcIqc)
    {
        // 移动端此前把完成结果回写到 wm_item_recpt_line（错误来源）且不校验数量，
        // 现统一走共享完成服务，与 PC 使用同一套来源、数量与并发规则。
        qcIqc.setInspector(getUsername());
        qcIqcCompletionService.save(qcIqc, getUsername());
        return AjaxResult.success();
    }

    /**
     * 删除来料检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:remove')")
    @Log(title = "来料检验单", businessType = BusinessType.DELETE)
    @Transactional
    @DeleteMapping("/{iqcIds}")
    public AjaxResult remove(@PathVariable Long[] iqcIds)
    {
        for (Long iqcId:iqcIds
        ) {
            QcIqc iqc = qcIqcService.selectQcIqcByIqcId(iqcId);
            if(!UserConstants.ORDER_STATUS_PREPARE.equals(iqc.getStatus())){
                return AjaxResult.error("只能删除草稿状态单据！");
            }
            qcIqcLineService.deleteByIqcId(iqcId);
            QcDefectRecord p2 = new QcDefectRecord();
            p2.setQcId(iqcId);
            p2.setQcType(UserConstants.QC_TYPE_IQC);
            qcDefectRecordService.deleteByQcIdAndType(p2);//删除对应的缺陷记录
        }
        return toAjax(qcIqcService.deleteQcIqcByIqcIds(iqcIds));
    }

    /**
     * 根据头信息生成行信息
     * @param iqc
     */
    private void generateLine(QcIqc iqc){
        QcTemplateIndex param = new QcTemplateIndex();
        param.setTemplateId(iqc.getTemplateId());
        List<QcTemplateIndex > indexs = qcTemplateIndexService.selectQcTemplateIndexList(param);
        if(CollUtil.isNotEmpty(indexs)){
            for (QcTemplateIndex index:indexs
            ) {
                QcIqcLine line = new QcIqcLine();
                line.setIqcId(iqc.getIqcId());
                line.setIndexId(index.getIndexId());
                line.setIndexCode(index.getIndexCode());
                line.setIndexName(index.getIndexName());
                line.setIndexType(index.getIndexType());
                line.setQcTool(index.getQcTool());
                line.setCheckMethod(index.getCheckMethod());
                line.setStanderVal(index.getStanderVal());
                line.setUnitOfMeasure(index.getUnitOfMeasure());
                line.setThresholdMax(index.getThresholdMax());
                line.setThresholdMin(index.getThresholdMin());
                line.setCrQuantity(BigDecimal.ZERO);
                line.setMajQuantity(BigDecimal.ZERO);
                line.setMajQuantity(BigDecimal.ZERO);
                qcIqcLineService.insertQcIqcLine(line);
            }
        }
    }

}
