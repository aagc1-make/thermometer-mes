package com.ktg.mes.wm.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.mes.wm.domain.WmTransaction;
import com.ktg.mes.wm.service.IWmTransactionService;

/**
 * 库存流水（库存事务）
 *
 * 需求 §2.4.9 库存现有量与批次查询：支持按物料、批次、仓库、库位、
 * 单据类型及起止日期查询每一笔库存变动，用于批次追溯与业务单据核对。
 * 流水由库存过账（采购入库/生产领料/生产消耗/产品产出/成品入库/销售出库）自动写入，
 * 本控制器仅提供查询与导出，不提供任何修改入口。
 *
 * @author ktg-mes
 */
@RestController
@RequestMapping("/mes/wm/transaction")
public class WmTransactionController extends BaseController
{
    @Autowired
    private IWmTransactionService wmTransactionService;

    /**
     * 查询库存流水列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmTransaction wmTransaction)
    {
        startPage();
        List<WmTransaction> list = wmTransactionService.selectWmTransactionList(wmTransaction);
        return getDataTable(list);
    }

    /**
     * 导出库存流水
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:export')")
    @Log(title = "库存流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmTransaction wmTransaction)
    {
        List<WmTransaction> list = wmTransactionService.selectWmTransactionList(wmTransaction);
        ExcelUtil<WmTransaction> util = new ExcelUtil<WmTransaction>(WmTransaction.class);
        util.exportExcel(response, list, "库存流水数据");
    }

    /**
     * 获取库存流水详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:transaction:query')")
    @GetMapping(value = "/{transactionId}")
    public AjaxResult getInfo(@PathVariable("transactionId") Long transactionId)
    {
        return AjaxResult.success(wmTransactionService.selectWmTransactionByTransactionId(transactionId));
    }
}
