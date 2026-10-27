package com.ktg.mes.wm.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.linkage.SalesNoticeLinkageService;
import com.ktg.mes.wm.domain.WmSalesNoticeLine;
import com.ktg.mes.wm.service.IWmSalesNoticeLineService;
import org.springframework.util.CollectionUtils;
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
import com.ktg.mes.wm.domain.WmSalesNotice;
import com.ktg.mes.wm.service.IWmSalesNoticeService;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.common.core.page.TableDataInfo;

/**
 * 发货通知单Controller
 * 
 * @author yinjinlu
 * @date 2025-03-14
 */
@RestController
@RequestMapping("/mes/wm/salesnotice")
public class WmSalesNoticeController extends BaseController
{
    @Autowired
    private IWmSalesNoticeService wmSalesNoticeService;

    @Autowired
    private IWmSalesNoticeLineService wmSalesNoticeLineService;

    @Autowired
    private SalesNoticeLinkageService salesNoticeLinkageService;

    /**
     * 查询发货通知单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesnotice:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmSalesNotice wmSalesNotice)
    {
        startPage();
        List<WmSalesNotice> list = wmSalesNoticeService.selectWmSalesNoticeList(wmSalesNotice);
        return getDataTable(list);
    }

    /**
     * 导出发货通知单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesnotice:export')")
    @Log(title = "发货通知单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmSalesNotice wmSalesNotice)
    {
        List<WmSalesNotice> list = wmSalesNoticeService.selectWmSalesNoticeList(wmSalesNotice);
        ExcelUtil<WmSalesNotice> util = new ExcelUtil<WmSalesNotice>(WmSalesNotice.class);
        util.exportExcel(response, list, "发货通知单数据");
    }

    /**
     * 获取发货通知单详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesnotice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable("noticeId") Long noticeId)
    {
        return AjaxResult.success(wmSalesNoticeService.selectWmSalesNoticeByNoticeId(noticeId));
    }

    /**
     * 新增发货通知单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesnotice:add')")
    @Log(title = "发货通知单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmSalesNotice wmSalesNotice)
    {
        return toAjax(wmSalesNoticeService.insertWmSalesNotice(wmSalesNotice));
    }

    /**
     * 修改发货通知单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesnotice:edit')")
    @Log(title = "发货通知单", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional
    public AjaxResult edit(@RequestBody WmSalesNotice wmSalesNotice)
    {
        List<WmSalesNoticeLine> lines = null;
        if(UserConstants.ORDER_STATUS_CONFIRMED.equals(wmSalesNotice.getStatus())){
            WmSalesNoticeLine param = new WmSalesNoticeLine();
            param.setNoticeId(wmSalesNotice.getNoticeId());
            lines = wmSalesNoticeLineService.selectWmSalesNoticeLineList(param);
            if(CollectionUtils.isEmpty(lines)){
                return AjaxResult.error("请添加发货通知明细");
            }
        }
        int rows = wmSalesNoticeService.updateWmSalesNotice(wmSalesNotice);
        if(rows > 0 && UserConstants.ORDER_STATUS_CONFIRMED.equals(wmSalesNotice.getStatus())){
            salesNoticeLinkageService.createDraft(wmSalesNotice, lines, getUsername());
        }
        return toAjax(rows);
    }

    /**
     * 删除发货通知单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesnotice:remove')")
    @Log(title = "发货通知单", businessType = BusinessType.DELETE)
    @Transactional
	@DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        for (Long noticeId : noticeIds){
            if(UserConstants.ORDER_STATUS_PREPARE.equals(wmSalesNoticeService.selectWmSalesNoticeByNoticeId(noticeId).getStatus())){
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return AjaxResult.error("只能删除草稿状态的单据！");
            }
        }

        return toAjax(wmSalesNoticeService.deleteWmSalesNoticeByNoticeIds(noticeIds));
    }
}
