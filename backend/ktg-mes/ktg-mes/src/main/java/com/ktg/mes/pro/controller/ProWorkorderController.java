package com.ktg.mes.pro.controller;

import cn.hutool.core.collection.CollUtil;
import com.ktg.common.annotation.Log;
import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.core.page.TableDataInfo;
import com.ktg.common.enums.BusinessType;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.StringUtils;
import com.ktg.common.utils.poi.ExcelUtil;
import com.ktg.mes.md.domain.MdItemBatchConfig;
import com.ktg.mes.md.domain.MdProductBom;
import com.ktg.mes.md.service.IMdItemBatchConfigService;
import com.ktg.mes.md.service.IMdProductBomService;
import com.ktg.mes.linkage.IssueDraftLinkageService;
import com.ktg.mes.linkage.ProductionNextStepLinkageService;
import com.ktg.mes.linkage.ProductReceiptLinkageService;
import com.ktg.mes.pro.controller.vo.SubWorkorderBatchRequest;
import com.ktg.mes.wm.mapper.ReceiptGuardMapper;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.system.service.CodeSequenceService;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.pro.domain.ProTask;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.domain.ProWorkorderBom;
import com.ktg.mes.pro.service.IProAutoSchedulingService;
import com.ktg.mes.pro.service.IProFeedbackService;
import com.ktg.mes.pro.service.IProTaskService;
import com.ktg.mes.pro.service.IProWorkorderBomService;
import com.ktg.mes.pro.service.IProWorkorderLineService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.utils.WmBarCodeUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生产工单Controller
 *
 * @author yinjinlu
 * @date 2022-05-09
 */
@RestController
@RequestMapping("/mes/pro/workorder")
public class ProWorkorderController extends BaseController
{
    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private IProWorkorderBomService proWorkorderBomService;

    @Autowired
    private IProWorkorderLineService proWorkorderLineService;

    @Autowired
    private IMdProductBomService mdProductBomService;

    @Autowired
    private IMdItemBatchConfigService mdItemBatchConfigService;

    @Autowired
    private IProTaskService proTaskService;

    @Autowired
    private IProAutoSchedulingService proAutoSchedulingService;

    @Autowired
    private IProFeedbackService proFeedbackService;

    @Autowired
    private ProductionNextStepLinkageService productionNextStepLinkageService;

    @Autowired
    private IssueDraftLinkageService issueDraftLinkageService;

    @Autowired
    private ProductReceiptLinkageService productReceiptLinkageService;

    @Autowired
    private WmBarCodeUtil wmBarCodeUtil;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Autowired
    private ReceiptGuardMapper receiptGuardMapper;

    @Autowired
    private PostingLockMapper postingLockMapper;

    /**
     * 查询生产工单列表
     */
    @GetMapping("/list")
    public TableDataInfo list(ProWorkorder proWorkorder)
    {
        if (!com.ktg.common.utils.SecurityUtils.isAdmin(getUserId())) {
            proWorkorder.setShowDeleted(null);
        }
        // 下拉远程搜索：带关键字时由 Mapper 直接检索并限制 50 条，不走分页；不带关键字保持原有分页列表
        if (StringUtils.isEmpty(proWorkorder.getKeyword())) {
            startPage();
        }
        List<ProWorkorder> list = proWorkorderService.selectProWorkorderList(proWorkorder);
        return getDataTable(list);
    }

    @GetMapping("/listWithTaskJson")
    public TableDataInfo listWithTaskJson(ProWorkorder proWorkorder){
        startPage();
        List<ProWorkorder> list = proWorkorderService.selectProWorkorderList(proWorkorder);
        Iterator<ProWorkorder> iterator = list.iterator();
        while (iterator.hasNext()){
            ProWorkorder workorder = iterator.next();
            List<ProTask> tasks = proTaskService.selectProTaskProcessViewByWorkorder(workorder.getWorkorderId());
            workorder.setTasks(tasks);
        }
        return getDataTable(list);
    }

    /**
     * 导出生产工单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:export')")
    @Log(title = "生产工单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProWorkorder proWorkorder)
    {
        List<ProWorkorder> list = proWorkorderService.selectProWorkorderList(proWorkorder);
        ExcelUtil<ProWorkorder> util = new ExcelUtil<ProWorkorder>(ProWorkorder.class);
        util.exportExcel(response, list, "生产工单数据");
    }

    /**
     * 获取生产工单详细信息
     */
    @GetMapping(value = "/{workorderId}")
    public AjaxResult getInfo(@PathVariable("workorderId") Long workorderId)
    {
        return AjaxResult.success(proWorkorderService.selectProWorkorderByWorkorderId(workorderId));
    }

    /**
     * 新增生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "生产工单", businessType = BusinessType.INSERT)
    @Transactional(rollbackFor = Exception.class)
    @PostMapping
    public AjaxResult add(@RequestBody ProWorkorder proWorkorder)
    {
        // 工单号由服务端统一取号；弹窗预取的合法单号原样沿用，避免展示号与入库号不一致
        proWorkorder.setWorkorderCode(codeSequenceService.ensureCode(UserConstants.CODE_RULE_WORKORDER, proWorkorder.getWorkorderCode()));
        if(UserConstants.NOT_UNIQUE.equals(proWorkorderService.checkWorkorderCodeUnique(proWorkorder))){
            return AjaxResult.error("生产工单编号已存在！");
        }

        //效验产品的批次属性是否有客户，有则客户必选
        MdItemBatchConfig batchConfig = mdItemBatchConfigService.getMdItemBatchConfigByItemId(proWorkorder.getProductId());
        if(StringUtils.isNotNull(batchConfig) && UserConstants.YES.equals(batchConfig.getClientFlag()) && StringUtils.isNull(proWorkorder.getClientId())){
            return AjaxResult.error("当前生产工单的产品批次属性包含客户信息，客户必选！");
        }

        if(proWorkorder.getParentId()==null || proWorkorder.getParentId()==0){
            proWorkorder.setAncestors("0");
        }
        proWorkorderService.insertProWorkorder(proWorkorder);

        Long workorderId = proWorkorder.getWorkorderId();
        generateBomLine(workorderId);
        proWorkorder.setCreateBy(getUsername());
        wmBarCodeUtil.generateBarCode(UserConstants.BARCODE_TYPE_WORKORDER,proWorkorder.getWorkorderId(),proWorkorder.getWorkorderCode(),proWorkorder.getWorkorderName());
        return AjaxResult.success(workorderId);
    }

    /**
     * 从父工单的 BOM 行批量生成半成品子工单。
     *
     * <p>服务端语义：</p>
     * <ul>
     *   <li>整批原子：任一行校验失败或建单失败，整批回滚，不会留下半批子工单；</li>
     *   <li>幂等：以“父工单 + 来源 BOM 行”为键，重复点击/并发/重试不会重复生成；</li>
     *   <li>校验：父工单存在且未完工/取消，BOM 行确实属于该父工单；</li>
     *   <li>权限：沿用生产工单新增权限。</li>
     * </ul>
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:add')")
    @Log(title = "生产工单-批量生成半成品工单", businessType = BusinessType.INSERT)
    @Transactional
    @PostMapping("/subBatch")
    public AjaxResult addSubBatch(@RequestBody SubWorkorderBatchRequest request)
    {
        if(request == null || request.getParentId() == null){
            return AjaxResult.error("请先保存父工单");
        }
        if(request.getBomLineIds() == null || request.getBomLineIds().isEmpty()){
            return AjaxResult.error("请选择要生成子工单的 BOM 行");
        }
        ProWorkorder parent = proWorkorderService.selectProWorkorderByWorkorderId(request.getParentId());
        if(parent == null){
            return AjaxResult.error("父工单不存在");
        }
        if(UserConstants.ORDER_STATUS_FINISHED.equals(parent.getStatus())
                || UserConstants.ORDER_STATUS_CANCELED.equals(parent.getStatus())){
            return AjaxResult.error("已完工或已取消的工单不能生成半成品子工单");
        }

        ProWorkorderBom bomQuery = new ProWorkorderBom();
        bomQuery.setWorkorderId(request.getParentId());
        List<ProWorkorderBom> boms = proWorkorderBomService.selectProWorkorderBomList(bomQuery);
        Map<Long, ProWorkorderBom> bomById = new HashMap<>();
        if(boms != null){
            for (ProWorkorderBom bom : boms) {
                bomById.put(bom.getLineId(), bom);
            }
        }

        List<Long> createdIds = new ArrayList<>();
        List<String> createdCodes = new ArrayList<>();
        int skipped = 0;
        for (Long bomLineId : new LinkedHashSet<>(request.getBomLineIds())) {
            if(bomLineId == null){
                continue;
            }
            ProWorkorderBom bom = bomById.get(bomLineId);
            if(bom == null){
                // 抛异常触发整批回滚：要么全部生成，要么一张都不生成。
                throw new ServiceException("BOM 行[" + bomLineId + "]不属于当前工单，已拒绝整批生成");
            }
            String linkageKey = "AUTO:SUB_WORKORDER:" + request.getParentId() + ":" + bomLineId;
            if(receiptGuardMapper.countSubWorkorder(linkageKey) > 0){
                skipped++;
                continue;
            }
            ProWorkorder child = new ProWorkorder();
            child.setWorkorderCode(codeSequenceService.generateCode(UserConstants.CODE_RULE_WORKORDER));
            child.setWorkorderName(bom.getItemName() + "【" + bom.getQuantity() + "】"
                    + (bom.getUnitName() != null ? bom.getUnitName() : ""));
            child.setProductId(bom.getItemId());
            child.setProductCode(bom.getItemCode());
            child.setProductName(bom.getItemName());
            child.setProductSpc(bom.getItemSpc());
            child.setUnitOfMeasure(bom.getUnitOfMeasure());
            child.setUnitName(bom.getUnitName());
            child.setQuantity(bom.getQuantity());
            // 子工单承接父工单交付及生产上下文，避免缺失必填交期或客户批次信息。
            child.setRequestDate(parent.getRequestDate());
            child.setWorkorderType(parent.getWorkorderType());
            child.setClientId(parent.getClientId());
            child.setClientCode(parent.getClientCode());
            child.setClientName(parent.getClientName());
            child.setParentId(request.getParentId());
            child.setAncestors((parent.getAncestors() == null ? "0" : parent.getAncestors())
                    + "," + request.getParentId());
            child.setStatus(UserConstants.ORDER_STATUS_PREPARE);
            child.setOrderSource("SUB_WORKORDER");
            child.setSourceCode(parent.getWorkorderCode());
            child.setAttr1(linkageKey);
            child.setCreateBy(getUsername());
            try {
                proWorkorderService.insertProWorkorder(child);
            } catch (org.springframework.dao.DuplicateKeyException concurrent) {
                // 并发双击：唯一约束保证只生成一次
                skipped++;
                continue;
            }
            generateBomLine(child.getWorkorderId());
            wmBarCodeUtil.generateBarCode(UserConstants.BARCODE_TYPE_WORKORDER,
                    child.getWorkorderId(), child.getWorkorderCode(), child.getWorkorderName());
            createdIds.add(child.getWorkorderId());
            createdCodes.add(child.getWorkorderCode());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("workorderIds", createdIds);
        data.put("workorderCodes", createdCodes);
        data.put("createdCount", createdIds.size());
        data.put("skippedCount", skipped);
        return AjaxResult.success(data);
    }

    /**
     * 修改生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping
    public AjaxResult edit(@RequestBody ProWorkorder proWorkorder)
    {
        postingLockMapper.workorder(proWorkorder.getWorkorderId());
        ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(proWorkorder.getWorkorderId());
        if (workorder == null) {
            return AjaxResult.error("工单不存在");
        }
        proWorkorder.setWorkorderCode(workorder.getWorkorderCode());
        proWorkorder.setSourceDocType(workorder.getSourceDocType());
        proWorkorder.setSourceDocId(workorder.getSourceDocId());
        proWorkorder.setSourceDocCode(workorder.getSourceDocCode());
        if ("SALES_ORDER".equals(workorder.getSourceDocType())) {
            if (proWorkorder.getProductId() != null && !proWorkorder.getProductId().equals(workorder.getProductId())) {
                return AjaxResult.error("销售订单关联工单的产品须与订单行一致");
            }
            proWorkorder.setSourceCode(workorder.getSourceCode());
        }
        if (proWorkorder.getProductId() == null) {
            proWorkorder.setProductId(workorder.getProductId());
        }
        if (proWorkorder.getQuantity() == null) {
            proWorkorder.setQuantity(workorder.getQuantity());
        }
        MdItemBatchConfig batchConfig = mdItemBatchConfigService.getMdItemBatchConfigByItemId(proWorkorder.getProductId());
        if (batchConfig != null && UserConstants.YES.equals(batchConfig.getClientFlag())
                && proWorkorder.getClientId() == null && workorder.getClientId() == null) {
            return AjaxResult.error("当前生产工单的产品批次属性包含客户信息，客户必选！");
        }

        int ret =proWorkorderService.updateProWorkorder(proWorkorder);
        //如果是产品和数量发生变化则需要重新生成BOM组成
        if(ret >0){
            if(workorder.getProductId().longValue() != proWorkorder.getProductId().longValue() ||
                    workorder.getQuantity().compareTo(proWorkorder.getQuantity())!=0){
                removeBomLine(proWorkorder.getWorkorderId());
                generateBomLine(proWorkorder.getWorkorderId());
            }
        }

        // 工单确认：按工艺路线一次性生成全部工序任务；任一步失败抛异常，工单确认整体回滚
        boolean confirmTransition = ret > 0
                && !UserConstants.ORDER_STATUS_CONFIRMED.equals(workorder.getStatus())
                && UserConstants.ORDER_STATUS_CONFIRMED.equals(proWorkorder.getStatus());
        if (confirmTransition) {
            ProWorkorder confirmed = proWorkorderService.selectProWorkorderByWorkorderId(proWorkorder.getWorkorderId());
            proAutoSchedulingService.scheduleOnConfirm(confirmed.getWorkorderId(), getUsername());
            // 自动排产等价于手工排产：同步准备领料草稿、清理「生产排产」提醒，失败不影响确认结果
            try {
                issueDraftLinkageService.createDraftIfNeeded(confirmed.getWorkorderId(), getUsername());
            } catch (Exception e) {
                logger.warn("自动排产后生成领料草稿失败：workorderId={}, 原因={}",
                        confirmed.getWorkorderId(), e.getMessage());
            }
            try {
                productionNextStepLinkageService.clearScheduleIfAllScheduled();
            } catch (Exception ignore) {
                // 提醒清理失败不影响工单确认
            }
        }

        // 工单确认（已下达）后：若还没排产，点亮「生产排产」，让计划员知道下一步动作
        if (ret > 0 && UserConstants.ORDER_STATUS_CONFIRMED.equals(proWorkorder.getStatus())) {
            try {
                productionNextStepLinkageService.remindScheduleIfNoTask(proWorkorder.getWorkorderId());
            } catch (Exception ignore) {
                // 提醒失败不影响工单保存
            }
        }

        return toAjax(ret);
    }

    /**
     * 删除生产工单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:remove')")
    @Log(title = "生产工单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{workorderIds}")
    @Transactional
    public AjaxResult remove(@PathVariable Long[] workorderIds)
    {
        for (Long id : workorderIds) {
            postingLockMapper.workorder(id);
            ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(id);
            if (workorder == null || !UserConstants.ORDER_STATUS_PREPARE.equals(workorder.getStatus())) {
                return AjaxResult.error("只能删除草稿状态单据！");
            }
            if ("SALES_ORDER".equals(workorder.getSourceDocType())) {
                return AjaxResult.error("此工单关联销售订单，不能单独删除");
            }
        }
        for (Long id : workorderIds) {
            removeBomLine(id);
        }
        return toAjax(proWorkorderService.deleteProWorkorderByWorkorderIds(workorderIds));
    }

    /**
     * 根据生产工单中的产品生成BOM物料行
     * @param workorderId
     */
    private void generateBomLine(Long workorderId){
        //先根据ID找到对应的产品
        ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(workorderId);

        //BOM 行归属到工单的默认成品行（一工单一行项目）
        Long defaultLineId = proWorkorderLineService.getDefaultLineId(workorderId);

        //根据产品找到BOM组成
        MdProductBom param = new MdProductBom();
        param.setItemId(workorder.getProductId());
        List<MdProductBom> boms = mdProductBomService.selectMdProductBomList(param);

        //生成BOM数据
        BigDecimal orderQuantitiy = workorder.getQuantity();
        if(CollUtil.isNotEmpty(boms)){
            for (MdProductBom bom:boms
                 ) {
                ProWorkorderBom workorderBom = new ProWorkorderBom();
                workorderBom.setWorkorderId(workorderId);
                workorderBom.setWorkorderLineId(defaultLineId);
                workorderBom.setItemId(bom.getBomItemId());
                workorderBom.setItemCode(bom.getBomItemCode());
                workorderBom.setItemName(bom.getBomItemName());
                workorderBom.setItemSpc(bom.getBomItemSpec());
                workorderBom.setItemOrProduct(bom.getItemOrProduct());
                workorderBom.setUnitOfMeasure(bom.getUnitOfMeasure());
                workorderBom.setUnitName(bom.getUnitName());
                workorderBom.setQuantity(orderQuantitiy.multiply(bom.getQuantity()));
                proWorkorderBomService.insertProWorkorderBom(workorderBom);
            }
        }
    }

    /**
     * 删除当前工单下所有BOM组成
     * @param workorderId
     */
    private void removeBomLine(Long workorderId){
        ProWorkorderBom param = new ProWorkorderBom();
        param.setWorkorderId(workorderId);
        proWorkorderBomService.deleteProWorkorderBomByWorkorderId(workorderId);
    }

    /**
     * 获取当前工单的物料需求清单
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:list')")
    @GetMapping("/listItems")
    public TableDataInfo listItemss(ProWorkorder proWorkorder)
    {
        List<MdProductBom> result = new ArrayList<MdProductBom>();
        ProWorkorderBom param = new ProWorkorderBom();
        param.setWorkorderId(proWorkorder.getWorkorderId());
        List<ProWorkorderBom> boms = proWorkorderBomService.selectProWorkorderBomList(param);
        if(!CollectionUtils.isEmpty(boms)){
            for ( ProWorkorderBom bom: boms
                 ) {
                MdProductBom theBom = new MdProductBom();
                theBom.setBomItemId(bom.getItemId());
                theBom.setBomItemName(bom.getItemName());
                theBom.setBomItemCode(bom.getItemCode());
                theBom.setItemOrProduct(bom.getItemOrProduct());
                theBom.setUnitOfMeasure(bom.getUnitOfMeasure());
                theBom.setUnitName(bom.getUnitName());
                theBom.setBomItemSpec(bom.getItemSpc());
                theBom.setQuantity(bom.getQuantity());
                result.addAll(getBoms(theBom,bom.getQuantity(),0));
            }
        }
        TableDataInfo dataTable = getDataTable(result);
        List<MdProductBom> rows = (List<MdProductBom>) dataTable.getRows();
        // 使用Map来合并对象
        Map<String, MdProductBom> map = new HashMap<>();
        for (MdProductBom bom : rows) {
            String bomItemCode = bom.getBomItemCode();
            if (map.containsKey(bomItemCode)) {
                MdProductBom existingObj = map.get(bomItemCode);
                existingObj.setQuantity(existingObj.getQuantity().add(bom.getQuantity()) );
            } else {
                map.put(bomItemCode, bom);
            }
        }
        // 从Map中获取去重且合并后的对象集合
        Collection<MdProductBom> mergedCollection = map.values();
        // （可选）将结果转换回List
        List<MdProductBom> mergedList = mergedCollection.stream().collect(Collectors.toList());
        dataTable.setRows(mergedList);
        return dataTable;
    }

    private List<MdProductBom> getBoms(MdProductBom item,BigDecimal quantity,int count){
        MdProductBom param = new MdProductBom();
        List<MdProductBom> results = new ArrayList<MdProductBom>();
        if(count >20){
            return results;
        }
        param.setItemId(item.getBomItemId());
        List<MdProductBom> boms = mdProductBomService.selectMdProductBomList(param);
        if(CollUtil.isNotEmpty(boms)){
            //最多20层依赖
            count ++;
            for (MdProductBom bomItem: boms
                 ) {
                bomItem.setQuantity(quantity.multiply(bomItem.getQuantity()));
                results.addAll(getBoms(bomItem,bomItem.getQuantity(),count));
            }
        }else{
            results.add(item);
        }
        return results;
    }


    /**
     * 完成工单
     * @param workorderId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/finish/{workorderId}")
    public AjaxResult dofinish(@PathVariable Long workorderId){
        ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(workorderId);
        if (workorder == null) {
            return AjaxResult.error("工单不存在");
        }
        if (UserConstants.ORDER_STATUS_FINISHED.equals(workorder.getStatus())) {
            return AjaxResult.success();
        }

        // 完工前必须有真实报工依据：已排产工序未报满时不允许完工，避免出现“已完工但报工 0/5”的流水线死结
        List<String> unfinishedProcesses = findUnfinishedProcesses(workorderId);
        if (!unfinishedProcesses.isEmpty()) {
            return AjaxResult.error("工单还有 " + unfinishedProcesses.size() + " 道工序未完成报工："
                    + String.join("、", unfinishedProcesses) + "，请先完成报工再完工");
        }

        // 修复：不再把所有任务直接置 FINISHED（那会伪造工序/报工完成）。
        // 手工完工只关闭工单本身；产品入库草稿由 ProductReceiptLinkageService
        // 依据“终末工序(包装)真实报工完成”的证据决定是否生成。
        workorder.setStatus(UserConstants.ORDER_STATUS_FINISHED); //更新工单的状态
        workorder.setFinishDate(new Date());
        proWorkorderService.updateProWorkorder(workorder);
        //手工完工同样触发产品入库草稿（与报工/IPQC 完工口径一致，幂等）
        productReceiptLinkageService.ensureDraftForFinishedWorkorder(workorderId, getUsername());
        return AjaxResult.success();
    }

    /**
     * 未完成报工的工序：按任务排产量与已执行报工累计量比对。
     * 已取消任务、无排产量的任务不计；只有真实报工达量才算该工序完成。
     */
    private List<String> findUnfinishedProcesses(Long workorderId) {
        ProTask taskQuery = new ProTask();
        taskQuery.setWorkorderId(workorderId);
        List<ProTask> tasks = proTaskService.selectProTaskList(taskQuery);
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        ProFeedback feedbackQuery = new ProFeedback();
        feedbackQuery.setWorkorderId(workorderId);
        List<ProFeedback> feedbacks = proFeedbackService.selectProFeedbackList(feedbackQuery);
        Map<Long, BigDecimal> reportedByTask = new HashMap<>();
        if (feedbacks != null) {
            for (ProFeedback feedback : feedbacks) {
                if (feedback.getTaskId() == null || !UserConstants.FEEDBACK_STATUS_FINISHED.equals(feedback.getStatus())) {
                    continue;
                }
                BigDecimal quantity = feedback.getQuantityFeedback() == null ? BigDecimal.ZERO : feedback.getQuantityFeedback();
                reportedByTask.merge(feedback.getTaskId(), quantity, BigDecimal::add);
            }
        }
        List<String> unfinished = new ArrayList<>();
        for (ProTask task : tasks) {
            if (UserConstants.ORDER_STATUS_CANCELED.equals(task.getStatus())
                    || task.getQuantity() == null || task.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal reported = reportedByTask.getOrDefault(task.getTaskId(), BigDecimal.ZERO);
            if (reported.compareTo(task.getQuantity()) < 0) {
                unfinished.add(StringUtils.isNotEmpty(task.getProcessName()) ? task.getProcessName() : task.getTaskCode());
            }
        }
        return unfinished;
    }

    /**
     * 领料草稿对账（幂等重试/补偿入口）：
     * 为“已确认、已排产但还没有领料草稿”的工单逐张补建草稿。重复调用不重复建单（唯一键兜底）。
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单-对账领料草稿", businessType = BusinessType.UPDATE)
    @PostMapping("/reconcileIssueDrafts")
    public AjaxResult reconcileIssueDrafts() {
        com.ktg.mes.linkage.IssueDraftReconcileResult result =
                productionNextStepLinkageService.ensureIssueDraftReady(null, getUsername());
        // 部分失败通过 result.failed / failedWorkorderIds 明确体现，调用方据此判断是否需要重试
        return AjaxResult.success(result);
    }

    /**
     * 取消工单
     * @param workorderId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('mes:pro:workorder:edit')")
    @Log(title = "生产工单", businessType = BusinessType.UPDATE)
    @Transactional
    @PutMapping("/cancel/{workorderId}")
    public AjaxResult doCancel(@PathVariable Long workorderId){
        ProWorkorder workorder = proWorkorderService.selectProWorkorderByWorkorderId(workorderId);

        //将此工单下所有的生产任务状态设置为取消
        ProTask param = new ProTask();
        param.setWorkorderId(workorderId);
        List<ProTask> tasks = proTaskService.selectProTaskList(param);
        if(!CollectionUtils.isEmpty(tasks)){
            for (ProTask task:tasks
            ) {
                task.setStatus(UserConstants.ORDER_STATUS_CANCELED);
                task.setCancelDate(new Date());
                proTaskService.updateProTask(task);
            }
        }

        workorder.setStatus(UserConstants.ORDER_STATUS_CANCELED); //更新工单的状态
        workorder.setCancelDate(new Date());
        proWorkorderService.updateProWorkorder(workorder);
        return AjaxResult.success();
    }


    /**
     * 首页生产工单查询
     * @param proWorkorder
     * @return
     */
    @GetMapping("/getHomeList")
    public AjaxResult getHomeList(ProWorkorder proWorkorder) {
        return proWorkorderService.getHomeList(proWorkorder);
    }

}
