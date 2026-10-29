package com.ktg.mes.dv.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.dv.domain.DvCheckRecordLine;
import com.ktg.mes.dv.domain.DvCheckSubject;
import com.ktg.mes.dv.service.IDvCheckRecordLineService;
import com.ktg.mes.dv.service.IDvCheckSubjectService;
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
import com.ktg.mes.dv.domain.DvCheckRecord;
import com.ktg.mes.dv.service.IDvCheckRecordService;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.common.core.page.TableDataInfo;

/**
 * 设备点检记录Controller
 * 
 * @author yinjinlu
 * @date 2024-12-26
 */
@RestController
@RequestMapping("/mes/dv/checkrecord")
public class DvCheckRecordController extends BaseController
{
    @Autowired
    private IDvCheckRecordService dvCheckRecordService;

    @Autowired
    private IDvCheckSubjectService dvCheckSubjectService;

    @Autowired
    private IDvCheckRecordLineService dvCheckRecordLineService;

    /**
     * 查询设备点检记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:dv:checkrecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(DvCheckRecord dvCheckRecord)
    {
        startPage();
        List<DvCheckRecord> list = dvCheckRecordService.selectDvCheckRecordList(dvCheckRecord);
        return getDataTable(list);
    }

    /**
     * 导出设备点检记录列表
     */
    @PreAuthorize("@ss.hasPermi('mes:dv:checkrecord:export')")
    @Log(title = "设备点检记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DvCheckRecord dvCheckRecord)
    {
        List<DvCheckRecord> list = dvCheckRecordService.selectDvCheckRecordList(dvCheckRecord);
        ExcelUtil<DvCheckRecord> util = new ExcelUtil<DvCheckRecord>(DvCheckRecord.class);
        util.exportExcel(response, list, "设备点检记录数据");
    }

    /**
     * 获取设备点检记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('mes:dv:checkrecord:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return AjaxResult.success(dvCheckRecordService.selectDvCheckRecordByRecordId(recordId));
    }

    /**
     * 新增设备点检记录
     */
    @PreAuthorize("@ss.hasPermi('mes:dv:checkrecord:add')")
    @Log(title = "设备点检记录", businessType = BusinessType.INSERT)
    @Transactional
    @PostMapping
    public AjaxResult add(@RequestBody DvCheckRecord dvCheckRecord)
    {
        dvCheckRecordService.insertDvCheckRecord(dvCheckRecord);

        if(dvCheckRecord.getPlanId()!= null){
            //根据选择的点检计划自动生成对应的行信息
            DvCheckSubject param = new DvCheckSubject();
            param.setPlanId(dvCheckRecord.getPlanId());
            List<DvCheckSubject> subjectList = dvCheckSubjectService.selectDvCheckSubjectList(param);
            if(!CollectionUtils.isEmpty(subjectList)){
                for(DvCheckSubject subject : subjectList){
                    DvCheckRecordLine line = new DvCheckRecordLine();
                    line.setRecordId(dvCheckRecord.getRecordId());
                    line.setSubjectId(subject.getSubjectId());
                    line.setSubjectName(subject.getSubjectName());
                    line.setSubjectType(subject.getSubjectType());
                    line.setSubjectContent(subject.getSubjectContent());
                    line.setSubjectStandard(subject.getSubjectStandard());
                    line.setCheckStatus(UserConstants.YES);
                    // 原实现只构造了行对象却没有落库，导致点检记录永远没有明细
                    dvCheckRecordLineService.insertDvCheckRecordLine(line);
                }
            }
        }

        return AjaxResult.success(dvCheckRecord.getRecordId());
    }

    /**
     * 修改设备点检记录
     */
    @PreAuthorize("@ss.hasPermi('mes:dv:checkrecord:edit')")
    @Log(title = "设备点检记录", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping
    public AjaxResult edit(@RequestBody DvCheckRecord dvCheckRecord)
    {
        if(UserConstants.ORDER_STATUS_FINISHED.equals(dvCheckRecord.getStatus())){
            DvCheckRecordLine param = new DvCheckRecordLine();
            param.setRecordId(dvCheckRecord.getRecordId());
            List<DvCheckRecordLine> lineList = dvCheckRecordLineService.selectDvCheckRecordLineList(param);
            if(CollectionUtils.isEmpty(lineList)){
                return AjaxResult.error("请添加设备点检项目结果信息");
            }
        }

        DvCheckRecord oldRecord = dvCheckRecordService.selectDvCheckRecordByRecordId(dvCheckRecord.getRecordId());
        if(oldRecord.getPlanId() != null && dvCheckRecord.getPlanId() != null && !dvCheckRecord.getPlanId().equals(oldRecord.getPlanId())){
            dvCheckRecordLineService.deleteDvCheckRecordLineByRecordId(dvCheckRecord.getRecordId());
            //根据选择的点检计划自动生成对应的行信息
            DvCheckSubject param = new DvCheckSubject();
            param.setPlanId(dvCheckRecord.getPlanId());
            List<DvCheckSubject> subjectList = dvCheckSubjectService.selectDvCheckSubjectList(param);
            if(!CollectionUtils.isEmpty(subjectList)){
                for(DvCheckSubject subject : subjectList){
                    DvCheckRecordLine line = new DvCheckRecordLine();
                    line.setRecordId(dvCheckRecord.getRecordId());
                    line.setSubjectId(subject.getSubjectId());
                    line.setSubjectName(subject.getSubjectName());
                    line.setSubjectType(subject.getSubjectType());
                    line.setSubjectContent(subject.getSubjectContent());
                    line.setSubjectStandard(subject.getSubjectStandard());
                    line.setCheckStatus(UserConstants.YES);
                    // 同上：换方案重建明细时必须真正插入，否则 delete 之后记录会变成“无明细”
                    dvCheckRecordLineService.insertDvCheckRecordLine(line);
                }
            }
        }

        return toAjax(dvCheckRecordService.updateDvCheckRecord(dvCheckRecord));
    }

    /**
     * 删除设备点检记录
     */
    @PreAuthorize("@ss.hasPermi('mes:dv:checkrecord:remove')")
    @Log(title = "设备点检记录", businessType = BusinessType.DELETE)
    @Transactional
	@DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {

        for(Long recordId : recordIds){
            dvCheckRecordLineService.deleteDvCheckRecordLineByRecordId(recordId);
        }

        return toAjax(dvCheckRecordService.deleteDvCheckRecordByRecordIds(recordIds));
    }
}
