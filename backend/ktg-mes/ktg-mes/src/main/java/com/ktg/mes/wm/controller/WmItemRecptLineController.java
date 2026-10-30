package com.ktg.mes.wm.controller;

import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ktg.common.utils.StringUtils;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.service.*;
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
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.common.core.page.TableDataInfo;

/**
 * 物料入库单行Controller
 * 
 * @author yinjinlu
 * @date 2022-05-22
 */
@RestController
@RequestMapping("/mes/wm/itemrecptline")
public class WmItemRecptLineController extends BaseController
{
    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IWmBatchService wmBatchService;

    @Autowired
    private com.ktg.mes.wm.service.impl.ItemRecptWorkflowService itemRecptWorkflowService;

    @Autowired
    private com.ktg.mes.wm.mapper.ReceiptGuardMapper receiptGuardMapper;


    /**
     * 查询物料入库单行列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecptLine wmItemRecptLine)
    {
        startPage();
        List<WmItemRecptLine> list = wmItemRecptLineService.selectWmItemRecptLineList(wmItemRecptLine);
        return getDataTable(list);
    }

    /**
     * 查询物料入库单行列表，携带对应的明细行信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:list')")
    @GetMapping("/listWithDetail")
    public TableDataInfo listWithDetail(WmItemRecptLine wmItemRecptLine){
        startPage();
        List<WmItemRecptLine> list = wmItemRecptLineService.selectWmItemRecptLineWithDetail(wmItemRecptLine);
        return getDataTable(list);
    }


    /**
     * 导出物料入库单行列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:export')")
    @Log(title = "物料入库单行", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmItemRecptLine wmItemRecptLine)
    {
        List<WmItemRecptLine> list = wmItemRecptLineService.selectWmItemRecptLineList(wmItemRecptLine);
        ExcelUtil<WmItemRecptLine> util = new ExcelUtil<WmItemRecptLine>(WmItemRecptLine.class);
        util.exportExcel(response, list, "物料入库单行数据");
    }

    /**
     * 获取物料入库单行详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:query')")
    @GetMapping(value = "/{lineId}")
    public AjaxResult getInfo(@PathVariable("lineId") Long lineId)
    {
        return AjaxResult.success(wmItemRecptLineService.selectWmItemRecptLineByLineId(lineId));
    }

    /**
     * 新增物料入库单行
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:add')")
    @Log(title = "物料入库单行", businessType = BusinessType.INSERT)
    @Transactional
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecptLine wmItemRecptLine)
    {
        //数量校验
        if(BigDecimal.ZERO.compareTo(wmItemRecptLine.getQuantityRecived()) >= 0){
            return AjaxResult.error("到货数量必须大于0!");
        }

        //入库行只能在草稿状态维护，且检验要求/检验单以来源到货行为准，避免伪造 IQC。
        itemRecptWorkflowService.assertLineMutable(wmItemRecptLine.getRecptId());
        WmArrivalNoticeLine sourceLine = itemRecptWorkflowService.bindSourceLine(wmItemRecptLine);
        assertLineQuantityAgainstSource(wmItemRecptLine, sourceLine, 0L);

        WmItemRecpt wmItemRecpt = wmItemRecptService.selectWmItemRecptByRecptId(wmItemRecptLine.getRecptId());

        //自动生成批次号
        WmBatch batch = new WmBatch();
        batch.setItemId(wmItemRecptLine.getItemId());
        batch.setItemCode(wmItemRecptLine.getItemCode());
        batch.setItemName(wmItemRecptLine.getItemName());
        batch.setSpecification(wmItemRecptLine.getSpecification());
        batch.setUnitOfMeasure(wmItemRecptLine.getUnitOfMeasure());
        batch.setUnitName(wmItemRecptLine.getUnitName());
        batch.setProduceDate(wmItemRecptLine.getProduceDate());
        batch.setExpireDate(wmItemRecptLine.getExpireDate());
        batch.setLotNumber(wmItemRecptLine.getLotNumber());
        batch.setPoCode(wmItemRecpt.getPoCode());
        batch.setVendorId(wmItemRecpt.getVendorId());
        batch.setVendorCode(wmItemRecpt.getVendorCode());
        batch.setVendorName(wmItemRecpt.getVendorName());
        batch.setVendorNick(wmItemRecpt.getVendorNick());

        WmBatch wmBatch = wmBatchService.getOrGenerateBatchCode(batch);
        if(StringUtils.isNotNull(wmBatch)){
            wmItemRecptLine.setBatchId(wmBatch.getBatchId());
            wmItemRecptLine.setBatchCode(wmBatch.getBatchCode());
        }else{
            wmItemRecptLine.setBatchId(null);
            wmItemRecptLine.setBatchCode(null);
        }
        wmItemRecptLine.setCreateBy(getUsername());
        wmItemRecptLineService.insertWmItemRecptLine(wmItemRecptLine);
        return AjaxResult.success(wmItemRecptLine);
    }

    /**
     * 修改物料入库单行
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:edit')")
    @Log(title = "物料入库单行", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping
    public AjaxResult edit(@RequestBody WmItemRecptLine wmItemRecptLine)
    {
        //数量校验
        if(BigDecimal.ZERO.compareTo(wmItemRecptLine.getQuantityRecived()) >= 0){
            return AjaxResult.error("到货数量必须大于0!");
        }

        itemRecptWorkflowService.assertLineMutable(wmItemRecptLine.getRecptId());
        WmArrivalNoticeLine sourceLine = itemRecptWorkflowService.bindSourceLine(wmItemRecptLine);
        assertLineQuantityAgainstSource(wmItemRecptLine, sourceLine, wmItemRecptLine.getLineId());

        WmItemRecpt wmItemRecpt = wmItemRecptService.selectWmItemRecptByRecptId(wmItemRecptLine.getRecptId());

        //自动生成批次号
        WmBatch batch = new WmBatch();
        batch.setItemId(wmItemRecptLine.getItemId());
        batch.setItemCode(wmItemRecptLine.getItemCode());
        batch.setItemName(wmItemRecptLine.getItemName());
        batch.setSpecification(wmItemRecptLine.getSpecification());
        batch.setUnitOfMeasure(wmItemRecptLine.getUnitOfMeasure());
        batch.setUnitName(wmItemRecptLine.getUnitName());
        batch.setProduceDate(wmItemRecptLine.getProduceDate());
        batch.setExpireDate(wmItemRecptLine.getExpireDate());
        batch.setLotNumber(wmItemRecptLine.getLotNumber());
        batch.setPoCode(wmItemRecpt.getPoCode());
        batch.setVendorId(wmItemRecpt.getVendorId());
        batch.setVendorCode(wmItemRecpt.getVendorCode());
        batch.setVendorName(wmItemRecpt.getVendorName());
        batch.setVendorNick(wmItemRecpt.getVendorNick());

        WmBatch wmBatch = wmBatchService.getOrGenerateBatchCode(batch);
        if(StringUtils.isNotNull(wmBatch)){
            wmItemRecptLine.setBatchId(wmBatch.getBatchId());
            wmItemRecptLine.setBatchCode(wmBatch.getBatchCode());
        }else{
            wmItemRecptLine.setBatchId(null);
            wmItemRecptLine.setBatchCode(null);
        }
        wmItemRecptLine.setUpdateBy(getUsername());
        wmItemRecptLineService.updateWmItemRecptLine(wmItemRecptLine);
        return AjaxResult.success(wmItemRecptLine);
    }

    /**
     * 删除物料入库单行
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:remove')")
    @Log(title = "物料入库单行", businessType = BusinessType.DELETE)
	@DeleteMapping("/{lineIds}")
    public AjaxResult remove(@PathVariable Long[] lineIds)
    {
        for (Long lineId : lineIds) {
            WmItemRecptLine line = wmItemRecptLineService.selectWmItemRecptLineByLineId(lineId);
            if (line == null) {
                return AjaxResult.error("入库行不存在");
            }
            itemRecptWorkflowService.assertLineMutable(line.getRecptId());
        }
        return toAjax(wmItemRecptLineService.deleteWmItemRecptLineByLineIds(lineIds));
    }

    /**
     * 入库行数量不得超过来源到货行的可入库数量（需检行以合格量为准，检验未完成时先按到货量控制草稿）。
     */
    private void assertLineQuantityAgainstSource(WmItemRecptLine line,
                                                  WmArrivalNoticeLine source,
                                                  Long excludeLineId) {
        itemRecptWorkflowService.assertLineQuantityWithinSource(line, source, excludeLineId);
    }
}
