package com.ktg.mes.wm.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.domain.tx.RtVendorTxBean;
import com.ktg.mes.wm.service.IStorageCoreService;
import com.ktg.mes.wm.service.impl.PreStockVendorReturnService;
import com.ktg.mes.wm.service.IWmRtVendorDetailService;
import com.ktg.mes.wm.service.IWmRtVendorLineService;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.impl.VendorReturnWorkflowService;
import com.ktg.mes.wm.service.impl.StockVendorReturnDraftService;
import org.apache.commons.collections.CollectionUtils;
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
import com.ktg.mes.wm.domain.WmRtVendor;
import com.ktg.mes.wm.service.IWmRtVendorService;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.common.core.page.TableDataInfo;

/**
 * 供应商退货Controller
 * 
 * @author yinjinlu
 * @date 2022-06-13
 */
@RestController
@RequestMapping("/mes/wm/rtvendor")
public class WmRtVendorController extends BaseController
{
    @Autowired private VendorReturnWorkflowService workflow;
    @Autowired private StockVendorReturnDraftService stockDrafts;
    @Autowired private PreStockVendorReturnService preStockDrafts;
    @Autowired private PostingLockMapper locks;
    @Autowired
    private IWmRtVendorService wmRtVendorService;

    @Autowired
    private IWmRtVendorLineService wmRtVendorLineService;

    @Autowired
    private IWmRtVendorDetailService wmRtVendorDetailService;

    @Autowired
    private IStorageCoreService storageCoreService;
    /**
     * 查询供应商退货列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtVendor wmRtVendor)
    {
        startPage();
        List<WmRtVendor> list = wmRtVendorService.selectWmRtVendorList(wmRtVendor);
        return getDataTable(list);
    }

    /**
     * 导出供应商退货列表
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:export')")
    @Log(title = "供应商退货", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WmRtVendor wmRtVendor)
    {
        List<WmRtVendor> list = wmRtVendorService.selectWmRtVendorList(wmRtVendor);
        ExcelUtil<WmRtVendor> util = new ExcelUtil<WmRtVendor>(WmRtVendor.class);
        util.exportExcel(response, list, "供应商退货数据");
    }

    /**
     * 获取供应商退货详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:query')")
    @GetMapping(value = "/{rtId}")
    public AjaxResult getInfo(@PathVariable("rtId") Long rtId)
    {
        return AjaxResult.success(wmRtVendorService.selectWmRtVendorByRtId(rtId));
    }

    @GetMapping("/checkQuantity/{rtId}")
    public AjaxResult checkQuantity(@PathVariable("rtId") Long rtId){

        WmRtVendorLine param = new WmRtVendorLine();
        param.setRtId(rtId);
        List<WmRtVendorLine> lines = wmRtVendorLineService.selectWmRtVendorLineList(param);
        boolean flag = true;
        if(CollectionUtils.isNotEmpty(lines)){
            for(WmRtVendorLine line : lines){
                if(!UserConstants.EQUAL_TO.equals(wmRtVendorDetailService.checkQuantity(line.getLineId()))){
                    flag = false;
                }
            }
        }
        return AjaxResult.success(flag);
    }


    /**
     * 新增供应商退货
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:add')")
    @Log(title = "供应商退货", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtVendor wmRtVendor)
    {
        if (wmRtVendor.getAttr1()!=null || wmRtVendor.getAttr2()!=null
                || (wmRtVendor.getStatus()!=null && !UserConstants.ORDER_STATUS_PREPARE.equals(wmRtVendor.getStatus())))
            return AjaxResult.error("新增退供单必须为普通草稿，来源需通过业务流程生成");
        wmRtVendor.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        if(UserConstants.NOT_UNIQUE.equals(wmRtVendorService.checkCodeUnique(wmRtVendor))){
            return AjaxResult.error("退货单编号已经存在！");
        }
        wmRtVendor.setCreateBy(getUsername());
        return toAjax(wmRtVendorService.insertWmRtVendor(wmRtVendor));
    }

    /**
     * 修改供应商退货
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:edit')")
    @Log(title = "供应商退货", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult edit(@RequestBody WmRtVendor wmRtVendor)
    {
        workflow.prepareEdit(wmRtVendor);
        if(UserConstants.NOT_UNIQUE.equals(wmRtVendorService.checkCodeUnique(wmRtVendor))){
            return AjaxResult.error("退货单编号已经存在！");
        }

        if(UserConstants.ORDER_STATUS_APPROVING.equals(wmRtVendor.getStatus())){
            WmRtVendorLine param = new WmRtVendorLine();
            param.setRtId(wmRtVendor.getRtId());
            List<WmRtVendorLine> lines = wmRtVendorLineService.selectWmRtVendorLineList(param);
            if(CollectionUtils.isEmpty(lines)){
                return AjaxResult.error("请添加退货单行!");
            }
        }

        return toAjax(wmRtVendorService.updateWmRtVendor(wmRtVendor));
    }

    /**
     * 删除供应商退货
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:remove')")
    @Log(title = "供应商退货", businessType = BusinessType.DELETE)
	@Transactional
    @DeleteMapping("/{rtIds}")
    public AjaxResult remove(@PathVariable Long[] rtIds)
    {
        java.util.SortedSet<Long> ordered=new java.util.TreeSet<Long>(java.util.Arrays.asList(rtIds));
        for (Long rtId:ordered) {
            if(!UserConstants.ORDER_STATUS_PREPARE.equals(locks.rtVendor(rtId))){
                return AjaxResult.error("只能删除草稿状态的单据!");
            }
        }
        for (Long rtId:ordered) {
            wmRtVendorLineService.deleteByRtId(rtId);
        }
        return toAjax(wmRtVendorService.deleteWmRtVendorByRtIds(rtIds));
    }

    /**
     * 执行退货
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:edit')")
    @Log(title = "供应商退货单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/{rtId}")
    public AjaxResult execute(@PathVariable Long rtId){
        workflow.execute(rtId);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:add')")
    @Log(title = "采购入库生成退供草稿", businessType = BusinessType.INSERT)
    @PostMapping("/fromReceipt/{recptId}")
    public AjaxResult fromReceipt(@PathVariable Long recptId) {
        return AjaxResult.success(stockDrafts.create(recptId, getUsername()));
    }

    /**
     * 按来料检验重新生成未入库退供草稿（取消/删除后剩余不合格量可再次生成）。
     */
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:add')")
    @Log(title = "来料不合格重新生成退供草稿", businessType = BusinessType.INSERT)
    @PostMapping("/fromIqc/{iqcId}")
    public AjaxResult fromIqc(@PathVariable Long iqcId) {
        Long rtId = preStockDrafts.createFromIqc(iqcId, getUsername());
        if (rtId == null) {
            return AjaxResult.error("该检验单的不合格数量已全部退供，无可退数量");
        }
        return AjaxResult.success(rtId);
    }


}
