package com.ktg.mes.wm.controller;

import com.ktg.mes.wm.mapper.PostingLockMapper;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.collection.CollUtil;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.domain.tx.ItemRecptTxBean;
import com.ktg.mes.wm.service.*;
import com.ktg.mes.wm.service.impl.ItemRecptWorkflowService;
import com.ktg.mes.linkage.FormLinkagePolicy;
import com.ktg.mes.linkage.MenuLinkageNoticePaths;
import com.ktg.mes.linkage.MenuLinkageNoticeService;
import com.ktg.mes.linkage.ProductionNextStepLinkageService;
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
 * 物料入库单Controller
 * 
 * @author yinjinlu
 * @date 2022-05-22
 */
@RestController
@RequestMapping("/mes/wm/itemrecpt")
public class WmItemRecptController extends BaseController
{
    @Autowired
    private PostingLockMapper postingLockMapper;

    @Autowired
    private ItemRecptWorkflowService itemRecptWorkflowService;

    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IWmItemRecptDetailService wmItemRecptDetailService;

    @Autowired
    private IWmArrivalNoticeService wmArrivalNoticeService;

    @Autowired
    private IWmArrivalNoticeLineService wmArrivalNoticeLineService;

    @Autowired
    private FormLinkagePolicy formLinkagePolicy;

    @Autowired
    private MenuLinkageNoticeService menuLinkageNoticeService;

    @Autowired
    private ProductionNextStepLinkageService productionNextStepLinkageService;

    @Autowired
    private IWmWarehouseService wmWarehouseService;

    @Autowired
    private IWmStorageLocationService wmStorageLocationService;

    @Autowired
    private IWmStorageAreaService wmStorageAreaService;

    @Autowired
    private IStorageCoreService storageCoreService;

    /**
     * 查询物料入库单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecpt wmItemRecpt)
    {
        startPage();
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(wmItemRecpt);
        return getDataTable(list);
    }

    /**
     * 导出物料入库单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:export')")
    @Log(title = "物料入库单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmItemRecpt wmItemRecpt)
    {
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(wmItemRecpt);
        ExcelUtil<WmItemRecpt> util = new ExcelUtil<WmItemRecpt>(WmItemRecpt.class);
        util.exportExcel(response, list, "物料入库单数据");
    }

    /**
     * 获取物料入库单详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmItemRecptService.selectWmItemRecptByRecptId(recptId));
    }

    /**
     * 新增物料入库单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:add')")
    @Log(title = "物料入库单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecpt wmItemRecpt)
    {
        if(UserConstants.NOT_UNIQUE.equals(wmItemRecptService.checkRecptCodeUnique(wmItemRecpt))){
            return  AjaxResult.error("编号已存在！");
        }

        wmItemRecpt.setCreateBy(getUsername());
        wmItemRecptService.insertWmItemRecpt(wmItemRecpt);
        return AjaxResult.success(wmItemRecpt);
    }

    /**
     * 确认入库单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:edit')")
    @Log(title = "物料入库单", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm")
    public AjaxResult confirm(@RequestBody WmItemRecpt wmItemRecpt){
        // 编制确认只允许 草稿 → 已确认，并且必须在数据库行锁内校验真实状态。
        itemRecptWorkflowService.confirm(wmItemRecpt.getRecptId(), getUsername());
        return AjaxResult.success();
    }


    /**
     * 修改物料入库单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:edit')")
    @Log(title = "物料入库单", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional
    public AjaxResult edit(@RequestBody WmItemRecpt wmItemRecpt)
    {
        if(UserConstants.NOT_UNIQUE.equals(wmItemRecptService.checkRecptCodeUnique(wmItemRecpt))){
            return  AjaxResult.error("编号已存在！");
        }

        // 状态机、来源冻结、上架数量校验统一在服务层完成（PC 与移动端共用）。
        itemRecptWorkflowService.edit(wmItemRecpt, getUsername(), false);
        return AjaxResult.success();
    }

    /**
     * 执行入库
     * @return
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:edit')")
    @Log(title = "物料入库单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/{recptId}")
    public AjaxResult execute(@PathVariable Long recptId){
        // 状态、来源、数量、检验放行、上架明细与过账幂等全部由共享服务在行锁内校验。
        itemRecptWorkflowService.execute(recptId, getUsername());
        return AjaxResult.success();
    }


    /**
     * 删除物料入库单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:remove')")
    @Log(title = "物料入库单", businessType = BusinessType.DELETE)
    @Transactional
	@DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        itemRecptWorkflowService.remove(recptIds, getUsername());
        return AjaxResult.success();
    }

    @GetMapping("/getItem/{vendorId}")
    @Log(title = "根据供应商Id查询物料数据")
    public AjaxResult getItem(@PathVariable Long vendorId) {
        return wmItemRecptService.getItem(vendorId);
    }

    @GetMapping("/getProcurementRecord/{vendorId}")
    @Log(title = "根据供应商Id查询采购记录")
    public AjaxResult getProcurementRecord(@PathVariable Long vendorId) {
        return wmItemRecptService.getProcurementRecord(vendorId);
    }
}
