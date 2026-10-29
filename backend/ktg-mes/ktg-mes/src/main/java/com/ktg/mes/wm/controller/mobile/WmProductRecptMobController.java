package com.ktg.mes.wm.controller.mobile;

import com.ktg.mes.wm.mapper.PostingLockMapper;
import cn.hutool.core.collection.CollUtil;
import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.domain.tx.ProductRecptTxBean;
import com.ktg.mes.wm.service.*;
import com.ktg.system.strategy.AutoCodeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Api("产品入库")
@RestController
@RequestMapping("/mobile/wm/productrecpt")
public class WmProductRecptMobController extends BaseController {

    @Autowired
    private PostingLockMapper postingLockMapper;

    @Autowired
    private IWmProductRecptService wmProductRecptService;

    @Autowired
    private IWmProductRecptLineService wmProductRecptLineService;

    @Autowired
    private IStorageCoreService storageCoreService;

    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private AutoCodeUtil autoCodeUtil;

    @Autowired
    private com.ktg.mes.wm.service.impl.ProductReceiptGuardService productReceiptGuardService;

    /**
     * 查询产品入库录列表
     */
    @ApiOperation("查询产品入库单清单接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductRecpt wmProductRecpt)
    {
        startPage();
        List<WmProductRecpt> list = wmProductRecptService.selectWmProductRecptList(wmProductRecpt);
        return getDataTable(list);
    }


    /**
     * 获取产品入库录详细信息
     */
    @ApiOperation("获取产品入库单明细信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmProductRecptService.selectWmProductRecptByRecptId(recptId));
    }

    /**
     * 新增产品入库记录
     */
    @ApiOperation("新增产品入库单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:add')")
    @Log(title = "产品入库记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductRecpt wmProductRecpt)
    {
        // 与 PC 一致：产品入库必须关联生产工单来源，无来源成品入库走杂项入库/盘点调整。
        if (wmProductRecpt.getWorkorderId() == null || wmProductRecpt.getWorkorderId() == 0L) {
            return AjaxResult.error("产品入库必须关联生产工单来源；无来源的成品入库请走杂项入库或盘点调整");
        }
        if(StringUtils.isNotNull(wmProductRecpt.getRecptCode())){
            if(UserConstants.NOT_UNIQUE.equals(wmProductRecptService.checkUnique(wmProductRecpt))){
                return AjaxResult.error("入库单编号已存在！");
            }
        }else {
            wmProductRecpt.setRecptCode(autoCodeUtil.genSerialCode(UserConstants.PRODUCTRECPT_CODE,""));
        }


        if(StringUtils.isNotNull(wmProductRecpt.getWorkorderId())){
            ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(wmProductRecpt.getWorkorderId());
            wmProductRecpt.setWorkorderCode(workorder.getWorkorderCode());
            wmProductRecpt.setWorkorderName(workorder.getWorkorderName());
        }

        wmProductRecpt.setCreateBy(getUsername());
        wmProductRecptService.insertWmProductRecpt(wmProductRecpt);
        return AjaxResult.success(wmProductRecpt);
    }

    /**
     * 修改产品入库录
     */
    @ApiOperation("编辑产品入库单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:edit')")
    @Log(title = "产品入库记录", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional
    public AjaxResult edit(@RequestBody WmProductRecpt wmProductRecpt)
    {
        String previousStatus = postingLockMapper.productRecpt(wmProductRecpt.getRecptId());
        if (previousStatus == null || "FINISHED".equals(previousStatus) || "FINISHED".equals(wmProductRecpt.getStatus())) {
            return AjaxResult.error("已执行单据不能修改，完成状态只能通过执行操作产生");
        }

        if(UserConstants.NOT_UNIQUE.equals(wmProductRecptService.checkUnique(wmProductRecpt))){
            return AjaxResult.error("入库单编号已存在！");
        }

        // 来源快照冻结：与 PC 端一致，来源工单/物料/自动单幂等键不允许通过普通编辑改指。
        WmProductRecpt persisted = wmProductRecptService.selectWmProductRecptByRecptId(wmProductRecpt.getRecptId());
        if (persisted != null) {
            if (persisted.getWorkorderId() != null && wmProductRecpt.getWorkorderId() != null
                    && !persisted.getWorkorderId().equals(wmProductRecpt.getWorkorderId())) {
                return AjaxResult.error("产品入库单的来源工单不允许通过普通编辑修改，如需更换请撤销后重建");
            }
            if (persisted.getItemId() != null && wmProductRecpt.getItemId() != null
                    && !persisted.getItemId().equals(wmProductRecpt.getItemId())) {
                return AjaxResult.error("产品入库单的物料不允许通过普通编辑修改，如需更换请撤销后重建");
            }
            if (persisted.getAttr1() != null && persisted.getAttr1().startsWith("AUTO:")
                    && wmProductRecpt.getAttr1() != null
                    && !persisted.getAttr1().equals(wmProductRecpt.getAttr1())) {
                return AjaxResult.error("自动生成的产品入库单不允许修改来源标识");
            }
        }

        if(StringUtils.isNotNull(wmProductRecpt.getWorkorderId())){
            ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(wmProductRecpt.getWorkorderId());
            wmProductRecpt.setWorkorderCode(workorder.getWorkorderCode());
            wmProductRecpt.setWorkorderName(workorder.getWorkorderName());
        }

        return toAjax(wmProductRecptService.updateWmProductRecpt(wmProductRecpt));
    }

    /**
     * 删除产品入库录
     */
    @ApiOperation("删除产品入库单接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:remove')")
    @Log(title = "产品入库记录", businessType = BusinessType.DELETE)
    @Transactional
    @DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        for (Long recptId: recptIds
        ) {
            WmProductRecpt recpt = wmProductRecptService.selectWmProductRecptByRecptId(recptId);
            if(!UserConstants.ORDER_STATUS_PREPARE.equals(recpt.getStatus())){
                return AjaxResult.error("只能删除草稿状态的单据!");
            }
            wmProductRecptLineService.deleteByRecptId(recptId);
        }
        return toAjax(wmProductRecptService.deleteWmProductRecptByRecptIds(recptIds));
    }

    /**
     * 执行入库
     * @return
     */
    @ApiOperation("产品入库单执行入库接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:edit')")
    @Log(title = "产品入库记录", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/{recptId}")
    public AjaxResult execute(@PathVariable Long recptId){
        String previousStatus = postingLockMapper.productRecpt(recptId);
        if (!"APPROVED".equals(previousStatus)) {
            return AjaxResult.error("单据不存在、已执行或尚未到可执行状态，请刷新后重试");
        }

        WmProductRecpt recpt = wmProductRecptService.selectWmProductRecptByRecptId(recptId);

        //单据有效性
        if(!StringUtils.isNotNull(recpt)){
            return AjaxResult.error("无效单据");
        }

        // 与 PC 一致：无来源产品入库默认拒绝
        if (recpt.getWorkorderId() == null || recpt.getWorkorderId() == 0L) {
            return AjaxResult.error("产品入库缺少可靠工单来源，禁止过账；无来源成品请走杂项入库或盘点调整");
        }

        //先检查单据状态
        if(UserConstants.ORDER_STATUS_FINISHED.equals(recpt.getStatus())){
            return AjaxResult.error("当前单据已提交!");
        }


        WmProductRecptLine param = new WmProductRecptLine();
        param.setRecptId(recptId);
        List<WmProductRecptLine> lines = wmProductRecptLineService.selectWmProductRecptLineList(param);
        if(CollUtil.isEmpty(lines)){
            return AjaxResult.error("请添加要入库的产品");
        }

        // 与 PC 端一致：锁工单 + 来源/数量/包装放行校验，移动端不能绕过
        postingLockMapper.workorder(recpt.getWorkorderId());
        productReceiptGuardService.assertWithinSource(recpt, lines);

        List<ProductRecptTxBean> beans = wmProductRecptService.getTxBean(recptId);
        storageCoreService.processProductRecpt(beans);

        recpt.setStatus(UserConstants.ORDER_STATUS_FINISHED);
        wmProductRecptService.updateWmProductRecpt(recpt);

        return AjaxResult.success();
    }

}
