package com.ktg.mes.dv.controller;

import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.dv.domain.DvCheckRecordLine;
import com.ktg.mes.dv.domain.DvCheckSubject;
import com.ktg.mes.dv.domain.DvCheckPlan;
import com.ktg.mes.dv.domain.DvCheckMachinery;
import com.ktg.mes.dv.service.IDvCheckPlanService;
import com.ktg.mes.dv.service.IDvCheckMachineryService;
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

    @Autowired
    private IDvCheckPlanService dvCheckPlanService;

    @Autowired
    private IDvCheckMachineryService dvCheckMachineryService;

    private String validatePlan(DvCheckRecord record) {
        if (record.getPlanId() == null || record.getMachineryId() == null ||
                record.getMachineryCode() == null || record.getMachineryCode().trim().isEmpty()) {
            return "请选择点检方案和设备";
        }
        DvCheckPlan plan = dvCheckPlanService.selectDvCheckPlanByPlanId(record.getPlanId());
        if (plan == null || !UserConstants.DV_PLAN_TYPE_CHECK.equals(plan.getPlanType()) ||
                !(UserConstants.ORDER_STATUS_FINISHED.equals(plan.getStatus()) ||
                  UserConstants.ORDER_STATUS_CONFIRMED.equals(plan.getStatus()))) {
            return "点检方案不存在或未启用";
        }
        DvCheckMachinery query = new DvCheckMachinery();
        query.setPlanId(record.getPlanId());
        query.setMachineryId(record.getMachineryId());
        query.setMachineryCode(record.getMachineryCode());
        if (CollectionUtils.isEmpty(dvCheckMachineryService.selectDvCheckMachineryList(query))) {
            return "所选设备不属于该点检方案";
        }
        record.setPlanCode(plan.getPlanCode());
        record.setPlanName(plan.getPlanName());
        record.setPlanType(plan.getPlanType());
        return null;
    }

    /** 提交完成前必须逐项填写点检结果：O/空 均视为未检查 */
    private String validateLineResults(List<DvCheckRecordLine> lineList) {
        if (CollectionUtils.isEmpty(lineList)) {
            return "请添加设备点检项目结果信息";
        }
        int unchecked = 0;
        for (DvCheckRecordLine line : lineList) {
            String status = line.getCheckStatus();
            if (status == null || status.trim().isEmpty() ||
                    UserConstants.DV_CHECK_STATUS_NOTCHECK.equals(status)) {
                unchecked++;
            }
        }
        if (unchecked > 0) {
            return "还有 " + unchecked + " 项点检结果未填写，请填写后再提交";
        }
        return null;
    }

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
        String error = validatePlan(dvCheckRecord);
        if (error != null) return AjaxResult.error(error);
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
                    line.setSubjectCode(subject.getSubjectCode());
                    line.setSubjectName(subject.getSubjectName());
                    line.setSubjectType(subject.getSubjectType());
                    line.setSubjectContent(subject.getSubjectContent());
                    line.setSubjectStandard(subject.getSubjectStandard());
                    line.setCheckStatus(UserConstants.DV_CHECK_STATUS_NOTCHECK);
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
            String lineError = validateLineResults(lineList);
            if (lineError != null) {
                return AjaxResult.error(lineError);
            }
        }

        DvCheckRecord oldRecord = dvCheckRecordService.selectDvCheckRecordByRecordId(dvCheckRecord.getRecordId());
        if (oldRecord == null) return AjaxResult.error("点检记录不存在");
        if (UserConstants.ORDER_STATUS_FINISHED.equals(dvCheckRecord.getStatus()) &&
                ((dvCheckRecord.getPlanId() != null && !Objects.equals(dvCheckRecord.getPlanId(), oldRecord.getPlanId())) ||
                 (dvCheckRecord.getMachineryId() != null && !Objects.equals(dvCheckRecord.getMachineryId(), oldRecord.getMachineryId())) ||
                 (dvCheckRecord.getMachineryCode() != null && !Objects.equals(dvCheckRecord.getMachineryCode(), oldRecord.getMachineryCode())))) {
            return AjaxResult.error("完成点检时不能更改方案或设备，请先保存修改并检查明细");
        }
        if (dvCheckRecord.getPlanId() != null || dvCheckRecord.getMachineryId() != null || dvCheckRecord.getMachineryCode() != null) {
            if (dvCheckRecord.getPlanId() == null) dvCheckRecord.setPlanId(oldRecord.getPlanId());
            if (dvCheckRecord.getMachineryId() == null) dvCheckRecord.setMachineryId(oldRecord.getMachineryId());
            if (dvCheckRecord.getMachineryCode() == null) dvCheckRecord.setMachineryCode(oldRecord.getMachineryCode());
            String error = validatePlan(dvCheckRecord);
            if (error != null) return AjaxResult.error(error);
        } else {
            dvCheckRecord.setPlanCode(oldRecord.getPlanCode());
            dvCheckRecord.setPlanName(oldRecord.getPlanName());
            dvCheckRecord.setPlanType(oldRecord.getPlanType());
        }
        if(dvCheckRecord.getPlanId() != null && !dvCheckRecord.getPlanId().equals(oldRecord.getPlanId())){
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
                    line.setSubjectCode(subject.getSubjectCode());
                    line.setSubjectName(subject.getSubjectName());
                    line.setSubjectType(subject.getSubjectType());
                    line.setSubjectContent(subject.getSubjectContent());
                    line.setSubjectStandard(subject.getSubjectStandard());
                    line.setCheckStatus(UserConstants.DV_CHECK_STATUS_NOTCHECK);
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
