package com.ktg.mes.wm.controller.mobile;

import cn.hutool.core.collection.CollUtil;
import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.utils.StringUtils;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.domain.tx.RtIssueTxBean;
import com.ktg.mes.wm.service.*;
import com.ktg.mes.qc.service.impl.RtIssueInspectionDraftService;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.service.impl.RtIssueExecuteService;
import com.ktg.system.strategy.AutoCodeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api("生产退料")
@RestController
@RequestMapping("/mobile/wm/rtissue")
public class WmRtIssueMobController extends BaseController {
    @Autowired
    private RtIssueExecuteService rtIssueExecuteService;
    @Autowired
    private PostingLockMapper postingLockMapper;

    @Autowired
    private IWmRtIssueService wmRtIssueService;
    @Autowired
    private RtIssueInspectionDraftService inspectionDrafts;

    @Autowired
    private IWmRtIssueLineService wmRtIssueLineService;

    @Autowired
    private IWmRtIssueDetailService wmRtIssueDetailService;

    @Autowired
    private IStorageCoreService storageCoreService;

    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private AutoCodeUtil autoCodeUtil;

    /**
     * 查询生产退料单头列表
     */
    @ApiOperation("查询生产退料单清单接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:list')")
    @GetMapping("/list")
    public TableDataInfo list(WmRtIssue wmRtIssue)
    {
        startPage();
        List<WmRtIssue> list = wmRtIssueService.selectWmRtIssueList(wmRtIssue);
        return getDataTable(list);
    }


    /**
     * 获取生产退料单头详细信息
     */
    @ApiOperation("获取生产退料单详情接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:query')")
    @GetMapping(value = "/{rtId}")
    public AjaxResult getInfo(@PathVariable("rtId") Long rtId)
    {
        return AjaxResult.success(wmRtIssueService.selectWmRtIssueByRtId(rtId));
    }

    /**
     * 新增生产退料单头
     */
    @ApiOperation("新增生产退料单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:add')")
    @Log(title = "生产退料单头", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WmRtIssue wmRtIssue)
    {
        if (wmRtIssue.getStatus() != null && !UserConstants.RTISSUE_STATUS_PREPARE.equals(wmRtIssue.getStatus())) {
            return AjaxResult.error("新增退料单必须为草稿状态");
        }
        wmRtIssue.setStatus(UserConstants.RTISSUE_STATUS_PREPARE);
        if(StringUtils.isNotNull(wmRtIssue.getRtCode())){
            if(UserConstants.NOT_UNIQUE.equals(wmRtIssueService.checkUnique(wmRtIssue))){
                return AjaxResult.error("退料单编号已存在");
            }
        }else {
            wmRtIssue.setRtCode(autoCodeUtil.genSerialCode(UserConstants.RTISSUE_CODE,""));
        }


        if(StringUtils.isNotNull(wmRtIssue.getWorkorderId())){
            ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(wmRtIssue.getWorkorderId());
            wmRtIssue.setWorkorderCode(workorder.getWorkorderCode());
        }

        wmRtIssue.setCreateBy(getUsername());
        wmRtIssueService.insertWmRtIssue(wmRtIssue);
        return AjaxResult.success(wmRtIssue);
    }

    /**
     * 修改生产退料单头
     */
    @ApiOperation("修改生产退料单基本信息接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:edit')")
    @Log(title = "生产退料单头", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult edit(@RequestBody WmRtIssue wmRtIssue)
    {
        String previous = wmRtIssue.getRtId() == null ? null : postingLockMapper.rtIssue(wmRtIssue.getRtId());
        if (previous == null || UserConstants.RTISSUE_STATUS_FINISHED.equals(previous)
                || UserConstants.RTISSUE_STATUS_CANCELED.equals(previous)
                || UserConstants.RTISSUE_STATUS_FINISHED.equals(wmRtIssue.getStatus())) {
            return AjaxResult.error("退料单不存在、已完成或已取消，完成状态只能通过执行产生");
        }
        if (UserConstants.RTISSUE_STATUS_UNEXECUTE.equals(wmRtIssue.getStatus())
                && !UserConstants.RTISSUE_STATUS_UNSTOCK.equals(previous)
                && !UserConstants.RTISSUE_STATUS_UNEXECUTE.equals(previous)) {
            return AjaxResult.error("退料单未完成检验和上架，不能提交待执行");
        }
        if (UserConstants.RTISSUE_STATUS_PREPARE.equals(wmRtIssue.getStatus())
                && !UserConstants.RTISSUE_STATUS_PREPARE.equals(previous)) {
            return AjaxResult.error("退料单已提交，不能退回草稿");
        }
        if (UserConstants.RTISSUE_STATUS_UNSTOCK.equals(wmRtIssue.getStatus())
                && !UserConstants.RTISSUE_STATUS_PREPARE.equals(previous)
                && !UserConstants.RTISSUE_STATUS_UNSTOCK.equals(previous)) {
            return AjaxResult.error("待检退料只能由检验完成后进入待上架");
        }
        if(UserConstants.NOT_UNIQUE.equals(wmRtIssueService.checkUnique(wmRtIssue))){
            return AjaxResult.error("退料单编号已存在");
        }

        WmRtIssueLine line = new WmRtIssueLine();
        line.setRtId(wmRtIssue.getRtId());
        List<WmRtIssueLine> lines = wmRtIssueLineService.selectWmRtIssueLineList(line);

        //草稿状态下可以根据头上的退料类型，更新行上物资的质量状态
        if(UserConstants.RTISSUE_STATUS_PREPARE.equals(wmRtIssue.getStatus())){
            //根据头上的退料类型，设置行上物资的质量状态
            for(WmRtIssueLine wmRtIssueLine : lines){
                //如果是余料退料，则行上物资的质量状态为合格；如果是废料/坏料退料，则行上物资的质量状态为不合格
                if(UserConstants.YES.equals(wmRtIssueLine.getQcFlag())){
                    wmRtIssueLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTTEST);
                }else{
                    if(UserConstants.RTISSUE_TYPE_RMR.equals(wmRtIssue.getRtType())){
                        wmRtIssueLine.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
                    }else{
                        wmRtIssueLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
                    }
                }
                wmRtIssueLineService.updateWmRtIssueLine(wmRtIssueLine);
            }
        }

        //如果是向“待上架"状态提交，则判断行里面是否有待检验的物资，如果是，则状态改为“待检验”
        if(UserConstants.RTISSUE_STATUS_UNSTOCK.equals(wmRtIssue.getStatus())){
            if(CollectionUtils.isNotEmpty(lines)){
                for(WmRtIssueLine wmRtIssueLine : lines){
                    if(UserConstants.QUALITY_STATUS_NOTTEST.equals(wmRtIssueLine.getQualityStatus())){
                        wmRtIssue.setStatus(UserConstants.RTISSUE_STATUS_UNCHECK);
                        break;
                    }
                }
            }else{
                return AjaxResult.error("请添加退料物资!");
            }
        }

        //如果是向“待执行退料”状态提交，则检查行上的数量与明细行上总数量是否一致
        if(UserConstants.RTISSUE_STATUS_UNEXECUTE.equals(wmRtIssue.getStatus())) {
            boolean flag = true;
            StringBuilder sb = new StringBuilder();
            if(CollectionUtils.isNotEmpty(lines)){
                for(WmRtIssueLine wmRtIssueLine : lines){
                    if(UserConstants.LESS_THAN.equals(wmRtIssueDetailService.checkQuantity(wmRtIssueLine.getLineId()))){
                        flag = false;
                        sb.append(wmRtIssueLine.getItemName()).append(wmRtIssueLine.getBatchCode()).append("未完成上架！");
                    }
                }
            }

            if(!flag){
                return AjaxResult.error(sb.toString());
            }
        }


        int rows = wmRtIssueService.updateWmRtIssue(wmRtIssue);
        if (rows == 1 && UserConstants.RTISSUE_STATUS_UNCHECK.equals(wmRtIssue.getStatus())) {
            // 提交待检：为需检且未检的退料行生成/关联 RQC 草稿（同一行不重复建单）
            try {
                inspectionDrafts.ensureDrafts(wmRtIssue.getRtId(), getUsername());
            } catch (Exception e) {
                throw new ServiceException("生成退料检验草稿失败：" + e.getMessage());
            }
        }
        return toAjax(rows);
    }

    /**
     * 删除生产退料单头
     */
    @ApiOperation("删除生产退料单接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:remove')")
    @Log(title = "生产退料单头", businessType = BusinessType.DELETE)
    @Transactional
    @DeleteMapping("/{rtIds}")
    public AjaxResult remove(@PathVariable Long[] rtIds)
    {
        java.util.SortedSet<Long> ordered = new java.util.TreeSet<Long>(java.util.Arrays.asList(rtIds));
        for (Long rtId : ordered) {
            if(!UserConstants.ORDER_STATUS_PREPARE.equals(postingLockMapper.rtIssue(rtId))){
                return AjaxResult.error("只能删除草稿状态的单据!");
            }
        }
        for (Long rtId : ordered) {
            wmRtIssueLineService.deleteByRtId(rtId);
        }
        return toAjax(wmRtIssueService.deleteWmRtIssueByRtIds(rtIds));
    }

    /**
     * 执行退料
     * @param rtId
     * @return
     */
    @ApiOperation("执行生产退料接口")
    @PreAuthorize("@ss.hasPermi('mes:wm:rtissue:edit')")
    @Log(title = "生产退料单头", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/{rtId}")
    public AjaxResult execute(@PathVariable Long rtId){
        rtIssueExecuteService.execute(rtId);
        return AjaxResult.success();
    }

}
