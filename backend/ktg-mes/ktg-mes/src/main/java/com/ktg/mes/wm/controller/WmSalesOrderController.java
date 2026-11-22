package com.ktg.mes.wm.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.mes.wm.domain.WmSalesOrder;
import com.ktg.mes.wm.service.IWmSalesOrderService;
import com.ktg.system.service.CodeSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户销售订单Controller
 *
 * @author ktg
 * @date 2026-11-14
 */
@RestController
@RequestMapping("/mes/wm/salesorder")
public class WmSalesOrderController extends BaseController
{
    @Autowired
    private IWmSalesOrderService wmSalesOrderService;

    @Autowired
    private CodeSequenceService codeSequenceService;

    /**
     * 查询客户订单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmSalesOrder wmSalesOrder)
    {
        startPage();
        List<WmSalesOrder> list = wmSalesOrderService.selectWmSalesOrderList(wmSalesOrder);
        return getDataTable(list);
    }

    /**
     * 导出客户订单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:export')")
    @Log(title = "客户订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmSalesOrder wmSalesOrder)
    {
        List<WmSalesOrder> list = wmSalesOrderService.selectWmSalesOrderList(wmSalesOrder);
        ExcelUtil<WmSalesOrder> util = new ExcelUtil<WmSalesOrder>(WmSalesOrder.class);
        util.exportExcel(response, list, "客户订单数据");
    }

    /**
     * 获取客户订单详细信息（含订单行）
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return AjaxResult.success(wmSalesOrderService.selectWmSalesOrderByOrderId(orderId));
    }

    /**
     * 新增客户订单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:add')")
    @Log(title = "客户订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmSalesOrder wmSalesOrder)
    {
        wmSalesOrder.setOrderCode(codeSequenceService.ensureCode(UserConstants.CODE_RULE_SALES_ORDER, wmSalesOrder.getOrderCode()));
        if (UserConstants.NOT_UNIQUE.equals(wmSalesOrderService.checkOrderCodeUnique(wmSalesOrder)))
        {
            return AjaxResult.error("订单编号已存在！");
        }
        wmSalesOrder.setCreateBy(getUsername());
        wmSalesOrderService.insertWmSalesOrder(wmSalesOrder);
        return AjaxResult.success(wmSalesOrder.getOrderId());
    }

    /**
     * 修改客户订单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:edit')")
    @Log(title = "客户订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WmSalesOrder wmSalesOrder)
    {
        if (UserConstants.NOT_UNIQUE.equals(wmSalesOrderService.checkOrderCodeUnique(wmSalesOrder)))
        {
            return AjaxResult.error("订单编号已存在！");
        }
        wmSalesOrder.setUpdateBy(getUsername());
        return toAjax(wmSalesOrderService.updateWmSalesOrder(wmSalesOrder));
    }

    /**
     * 删除客户订单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:remove')")
    @Log(title = "客户订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(wmSalesOrderService.deleteWmSalesOrderByOrderIds(orderIds));
    }

    /**
     * 确认下单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:confirm')")
    @Log(title = "客户订单-确认下单", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{orderId}")
    public AjaxResult confirm(@PathVariable Long orderId)
    {
        return toAjax(wmSalesOrderService.confirmOrder(orderId, getUsername()));
    }

    /**
     * 一键生成生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:workorder')")
    @Log(title = "客户订单-生成生产工单", businessType = BusinessType.INSERT)
    @PostMapping("/generateWorkorder/{orderId}")
    public AjaxResult generateWorkorder(@PathVariable Long orderId)
    {
        return AjaxResult.success(wmSalesOrderService.generateWorkorders(orderId, getUsername()));
    }

    /**
     * 生成出货检验单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:oqc')")
    @Log(title = "客户订单-生成质检单", businessType = BusinessType.INSERT)
    @PostMapping("/generateOqc/{orderId}")
    public AjaxResult generateOqc(@PathVariable Long orderId)
    {
        return AjaxResult.success(wmSalesOrderService.generateOqc(orderId, getUsername()));
    }

    /**
     * 生成发货通知单
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:salesorder:notice')")
    @Log(title = "客户订单-生成发货单", businessType = BusinessType.INSERT)
    @PostMapping("/generateNotice/{orderId}")
    public AjaxResult generateNotice(@PathVariable Long orderId)
    {
        return AjaxResult.success(wmSalesOrderService.generateNotice(orderId, getUsername()));
    }
}
