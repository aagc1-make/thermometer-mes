package com.ktg.mes.wm.controller;

import com.ktg.mes.wm.mapper.PostingLockMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.collection.CollUtil;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.domain.tx.ProductRecptTxBean;
import com.ktg.mes.wm.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
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
 * 产品入库录Controller
 * 
 * @author yinjinlu
 * @date 2022-09-22
 */
@RestController
@RequestMapping("/mes/wm/productrecpt")
public class WmProductRecptController extends BaseController
{
    @Autowired
    private PostingLockMapper postingLockMapper;

    @Autowired
    private IWmProductRecptService wmProductRecptService;

    @Autowired
    private IWmProductRecptLineService wmProductRecptLineService;

    @Autowired
    private IWmProductRecptDetailService wmProductRecptDetailService;

    @Autowired
    private IStorageCoreService storageCoreService;

    @Autowired
    private com.ktg.mes.wm.mapper.ProductReceiptGuardMapper productReceiptGuardMapper;

    @Autowired
    private com.ktg.mes.wm.service.impl.ProductReceiptGuardService productReceiptGuardService;

    /**
     * 查询产品入库录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmProductRecpt wmProductRecpt)
    {
        startPage();
        List<WmProductRecpt> list = wmProductRecptService.selectWmProductRecptList(wmProductRecpt);
        return getDataTable(list);
    }

    /**
     * 导出产品入库录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:export')")
    @Log(title = "产品入库记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmProductRecpt wmProductRecpt)
    {
        List<WmProductRecpt> list = wmProductRecptService.selectWmProductRecptList(wmProductRecpt);
        ExcelUtil<WmProductRecpt> util = new ExcelUtil<WmProductRecpt>(WmProductRecpt.class);
        util.exportExcel(response, list, "产品入库录数据");
    }

    /**
     * 获取产品入库录详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:query')")
    @GetMapping(value = "/{recptId}")
    public AjaxResult getInfo(@PathVariable("recptId") Long recptId)
    {
        return AjaxResult.success(wmProductRecptService.selectWmProductRecptByRecptId(recptId));
    }


    @GetMapping("/checkQuantity/{recptId}")
    public AjaxResult checkQuantity(@PathVariable("recptId") Long recptId){

        WmProductRecptLine param = new WmProductRecptLine();
        param.setRecptId(recptId);
        List<WmProductRecptLine> lines = wmProductRecptLineService.selectWmProductRecptLineList(param);
        boolean flag = true;
        if(CollectionUtils.isNotEmpty(lines)){
            for(WmProductRecptLine line : lines){
                if(!UserConstants.EQUAL_TO.equals(wmProductRecptDetailService.checkQuantity(line.getLineId()))){
                    flag = false;
                }
            }
        }
        return AjaxResult.success(flag);
    }

    /**
     * 新增产品入库录
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:add')")
    @Log(title = "产品入库记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmProductRecpt wmProductRecpt)
    {
        // 产品入库必须有可靠来源：需求口径为“真实、已放行、尚未入库的合格产出”，
        // 无工单来源的手工/历史成品入库不属于产品入库业务（应走杂项入库/盘点调整）。
        if (wmProductRecpt.getWorkorderId() == null || wmProductRecpt.getWorkorderId() == 0L) {
            return AjaxResult.error("产品入库必须关联生产工单来源；无来源的成品入库请走杂项入库或盘点调整");
        }
        if(UserConstants.NOT_UNIQUE.equals(wmProductRecptService.checkUnique(wmProductRecpt))){
            return AjaxResult.error("入库单编号已存在！");
        }

        wmProductRecpt.setCreateBy(getUsername());
        return toAjax(wmProductRecptService.insertWmProductRecpt(wmProductRecpt));
    }

    /**
     * 修改产品入库录
     */
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

        // 来源快照冻结：来源工单/成品物料一经生成不允许通过普通编辑改指到别的工单或物料，
        // 否则可换到产出更多的工单来绕过执行时的来源上限校验。
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

        //行不能为空校验
        if(UserConstants.ORDER_STATUS_APPROVING.equals(wmProductRecpt.getStatus())){

            WmProductRecptLine param = new WmProductRecptLine();
            param.setRecptId(wmProductRecpt.getRecptId());
            List<WmProductRecptLine> lines = wmProductRecptLineService.selectWmProductRecptLineList(param);
            if(CollectionUtils.isEmpty(lines)){
                return AjaxResult.error("请添加产品入库单行!");
            }
        }

        return toAjax(wmProductRecptService.updateWmProductRecpt(wmProductRecpt));
    }

    /**
     * 删除产品入库录
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:productrecpt:remove')")
    @Log(title = "产品入库记录", businessType = BusinessType.DELETE)
    @Transactional
	@DeleteMapping("/{recptIds}")
    public AjaxResult remove(@PathVariable Long[] recptIds)
    {
        for (Long recptId: recptIds
             ) {
            // 仅草稿状态可删除（原逻辑取反，导致草稿删不掉、已完成的却能删）
            if(!UserConstants.ORDER_STATUS_PREPARE.equals(wmProductRecptService.selectWmProductRecptByRecptId(recptId).getStatus())){
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return AjaxResult.error("只能删除草稿状态的单据！");
            }

            wmProductRecptLineService.deleteByRecptId(recptId);
        }
        return toAjax(wmProductRecptService.deleteWmProductRecptByRecptIds(recptIds));
    }

    /**
     * 执行入库
     * @return
     */
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
        if (recpt == null) {
            return AjaxResult.error("入库单不存在");
        }
        // 无来源产品入库默认拒绝：产品入库必须以已放行合格产出为来源，
        // 不允许无工单的单据过账（否则可凭空增加成品库存并造成线边库负库存）。
        if (recpt.getWorkorderId() == null || recpt.getWorkorderId() == 0L) {
            return AjaxResult.error("产品入库缺少可靠工单来源，禁止过账；无来源成品请走杂项入库或盘点调整");
        }

        WmProductRecptLine param = new WmProductRecptLine();
        param.setRecptId(recptId);
        List<WmProductRecptLine> lines = wmProductRecptLineService.selectWmProductRecptLineList(param);
        if(CollUtil.isEmpty(lines)){
            return AjaxResult.error("请添加要入库的产品");
        }

        // 锁定工单这一共同资源：不同产品入库单并发过账时串行，避免超量
        postingLockMapper.workorder(recpt.getWorkorderId());

        // 按来源校验（不依赖 attr1，改 attr1 无法绕过）
        productReceiptGuardService.assertWithinSource(recpt, lines);

        List<ProductRecptTxBean> beans = wmProductRecptService.getTxBean(recptId);
        storageCoreService.processProductRecpt(beans);

        recpt.setStatus(UserConstants.ORDER_STATUS_FINISHED);
        wmProductRecptService.updateWmProductRecpt(recpt);

        return AjaxResult.success();
    }


}
