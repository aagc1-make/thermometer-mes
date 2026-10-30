package com.ktg.mes.wm.controller.mobile;

import com.itextpdf.text.pdf.PRIndirectReference;
import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.md.domain.MdVendor;
import com.ktg.mes.md.service.IMdVendorService;
import com.ktg.mes.wm.domain.WmRtVendor;
import com.ktg.mes.wm.domain.WmRtVendorLine;
import com.ktg.mes.wm.domain.tx.RtVendorTxBean;
import com.ktg.mes.wm.service.IStorageCoreService;
import com.ktg.mes.wm.service.IWmRtVendorDetailService;
import com.ktg.mes.wm.service.IWmRtVendorLineService;
import com.ktg.mes.wm.service.IWmRtVendorService;
import com.ktg.mes.wm.service.impl.VendorReturnWorkflowService;
import com.ktg.mes.wm.service.impl.StockVendorReturnDraftService;
import com.ktg.mes.wm.service.impl.PreStockVendorReturnService;
import com.ktg.system.strategy.AutoCodeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("退回供应商")
@RestController
@RequestMapping("/mobile/wm/rtvendor")
public class WmRtVendorMobController extends BaseController {
    @Autowired private VendorReturnWorkflowService workflow;
    @Autowired private StockVendorReturnDraftService stockDrafts;
    @Autowired private PreStockVendorReturnService preStockDrafts;

    @Autowired
    private IWmRtVendorService wmRtVendorService;

    @Autowired
    private IWmRtVendorLineService wmRtVendorLineService;

    @Autowired
    private IWmRtVendorDetailService wmRtVendorDetailService;

    @Autowired
    private IStorageCoreService storageCoreService;

    @Autowired
    private IMdVendorService mdVendorService;

    @Autowired
    private AutoCodeUtil autoCodeUtil;

    /**
     * 查询供应商退货列表
     */
    @ApiOperation("查询采购退货单据清单")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtVendor wmRtVendor)
    {
        startPage();
        List<WmRtVendor> list = wmRtVendorService.selectWmRtVendorList(wmRtVendor);
        return getDataTable(list);
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
     * 获取供应商退货详细信息
     */
    @ApiOperation("查询采购退货单据明细")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:query')")
    @GetMapping(value = "/{rtId}")
    public AjaxResult getInfo(@PathVariable("rtId") Long rtId)
    {
        return AjaxResult.success(wmRtVendorService.selectWmRtVendorByRtId(rtId));
    }

    /**
     * 新增供应商退货
     */
    @ApiOperation("新增采购退货单")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:add')")
    @Log(title = "供应商退货", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtVendor wmRtVendor)
    {
        if (wmRtVendor.getAttr1()!=null || wmRtVendor.getAttr2()!=null
                || (wmRtVendor.getStatus()!=null && !UserConstants.ORDER_STATUS_PREPARE.equals(wmRtVendor.getStatus())))
            return AjaxResult.error("新增退供单必须为普通草稿，来源需通过业务流程生成");
        wmRtVendor.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        if(StringUtils.isNotNull(wmRtVendor.getRtCode())){
            if(UserConstants.NOT_UNIQUE.equals(wmRtVendorService.checkCodeUnique(wmRtVendor))){
                return AjaxResult.error("退货单编号已经存在！");
            }
        }else {
            wmRtVendor.setRtCode(autoCodeUtil.genSerialCode(UserConstants.WM_RTVENDOR_CODE,""));
        }

        if(StringUtils.isNotNull(wmRtVendor.getVendorId())){
            MdVendor vendor = mdVendorService.selectMdVendorByVendorId(wmRtVendor.getVendorId());
            wmRtVendor.setVendorCode(vendor.getVendorCode());
            wmRtVendor.setVendorName(vendor.getVendorName());
            wmRtVendor.setVendorNick(vendor.getVendorNick());
        }

        if(StringUtils.isEmpty(wmRtVendor.getRtName())){
            wmRtVendor.setRtName(wmRtVendor.getVendorName()+"采购退货");
        }

        wmRtVendor.setCreateBy(getUsername());
        wmRtVendorService.insertWmRtVendor(wmRtVendor);
        return AjaxResult.success(wmRtVendor);
    }

    /**
     * 修改供应商退货
     */
    @ApiOperation("编辑采购退货单")
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

        if(StringUtils.isNotNull(wmRtVendor.getVendorId()) && wmRtVendor.getAttr2()==null){
            MdVendor vendor = mdVendorService.selectMdVendorByVendorId(wmRtVendor.getVendorId());
            wmRtVendor.setVendorCode(vendor.getVendorCode());
            wmRtVendor.setVendorName(vendor.getVendorName());
            wmRtVendor.setVendorNick(vendor.getVendorNick());
        }

        if(StringUtils.isEmpty(wmRtVendor.getRtName())){
            wmRtVendor.setRtName(wmRtVendor.getVendorName()+"采购退货");
        }

        wmRtVendor.setUpdateBy(getUsername());
        return toAjax(wmRtVendorService.updateWmRtVendor(wmRtVendor));
    }


    /**
     * 执行退货
     */
    @ApiOperation("执行退货")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:edit')")
    @Log(title = "供应商退货单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/{rtId}")
    public AjaxResult execute(@PathVariable Long rtId){
        workflow.execute(rtId);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mes:wm:rtvendor:add')")
    @PostMapping("/fromReceipt/{recptId}")
    public AjaxResult fromReceipt(@PathVariable Long recptId) {
        return AjaxResult.success(stockDrafts.create(recptId, getUsername()));
    }

    /**
     * 按来料检验重新生成未入库退供草稿（与 PC 同一规则）。
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
