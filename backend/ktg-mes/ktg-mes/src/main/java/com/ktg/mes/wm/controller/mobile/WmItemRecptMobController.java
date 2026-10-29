package com.ktg.mes.wm.controller.mobile;

import com.ktg.mes.wm.mapper.PostingLockMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.domain.tx.ItemRecptTxBean;
import com.ktg.mes.wm.service.*;
import com.ktg.system.strategy.AutoCodeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("采购入库")
@RestController
@RequestMapping("/mobile/wm/itemrecpt")
public class WmItemRecptMobController extends BaseController {

    @Autowired
    private PostingLockMapper postingLockMapper;

    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IWmItemRecptDetailService wmItemRecptDetailService;

    @Autowired
    private IWmArrivalNoticeService wmArrivalNoticeService;

    @Autowired
    private IStorageCoreService storageCoreService;

    @Autowired
    private com.ktg.mes.wm.service.impl.ItemRecptWorkflowService itemRecptWorkflowService;

    @Autowired
    private AutoCodeUtil autoCodeUtil;

    /**
     * 查询物料入库单列表
     */
    @ApiOperation("查询物料入库单列表")
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmItemRecpt wmItemRecpt)
    {
        startPage();
        List<WmItemRecpt> list = wmItemRecptService.selectWmItemRecptList(wmItemRecpt);
        return getDataTable(list);
    }



    /**
     * 新增物料入库单
     */
    @ApiOperation("新增采购入库单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:add')")
    @Log(title = "物料入库单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmItemRecpt wmItemRecpt)
    {
        if(StringUtils.isNotNull(wmItemRecpt.getRecptCode())){
            if(UserConstants.NOT_UNIQUE.equals(wmItemRecptService.checkRecptCodeUnique(wmItemRecpt))){
                return  AjaxResult.error("单据编号已存在！");
            }
        }else {
            wmItemRecpt.setRecptCode(autoCodeUtil.genSerialCode(UserConstants.ITEMRECPT_CODE,""));
        }

        if(StringUtils.isNull(wmItemRecpt.getVendorId())){
            return AjaxResult.error("请选择供应商！");
        }

        if(StringUtils.isNull(wmItemRecpt.getRecptName())){
            wmItemRecpt.setRecptName("供应商"+wmItemRecpt.getVendorName()+"物资入库");
        }



        wmItemRecpt.setCreateBy(getUsername());
        wmItemRecptService.insertWmItemRecpt(wmItemRecpt);
        return AjaxResult.success(wmItemRecpt);
    }

    /**
     * 修改物料入库单
     */
    @ApiOperation("修改采购入库单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:edit')")
    @Log(title = "物料入库单", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional
    public AjaxResult edit(@RequestBody WmItemRecpt wmItemRecpt)
    {
        if(UserConstants.NOT_UNIQUE.equals(wmItemRecptService.checkRecptCodeUnique(wmItemRecpt))){
            return  AjaxResult.error("单据编号已存在！");
        }
        // 与 PC 共用同一套状态机、来源冻结和上架数量校验（移动端不能绕过）。
        itemRecptWorkflowService.edit(wmItemRecpt, getUsername(), true);
        return AjaxResult.success();
    }

    /**
     * 获取物料入库单详细信息
     */
    @ApiOperation("获取物料入库单详细信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmItemRecptService.selectWmItemRecptByRecptId(recptId));
    }


    /**
     * 删除物料入库单
     */
    @ApiOperation("删除采购入库单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:remove')")
    @Log(title = "物料入库单", businessType = BusinessType.DELETE)
    @Transactional
    @DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        itemRecptWorkflowService.remove(recptIds, getUsername());
        return AjaxResult.success();
    }

    /**
     * 执行入库
     * @return
     */
    @ApiOperation("执行入库接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:itemrecpt:edit')")
    @Log(title = "物料入库单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/{recptId}")
    public AjaxResult execute(@PathVariable Long recptId){
        // 移动端此前只校验状态、未校验 IQC 与数量，现与 PC 共用同一执行服务。
        itemRecptWorkflowService.execute(recptId, getUsername());
        return AjaxResult.success();
    }


}
