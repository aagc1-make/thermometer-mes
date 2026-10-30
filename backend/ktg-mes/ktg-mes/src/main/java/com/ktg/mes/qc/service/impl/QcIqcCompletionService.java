package com.ktg.mes.qc.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.StringUtils;
import com.ktg.common.utils.bean.BeanUtils;
import com.ktg.mes.linkage.FormLinkagePolicy;
import com.ktg.mes.linkage.PurchaseReceiptLinkageService;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmOutsourceRecpt;
import com.ktg.mes.wm.domain.WmOutsourceRecptLine;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.ReceiptGuardMapper;
import com.ktg.mes.wm.service.IWmArrivalNoticeLineService;
import com.ktg.mes.wm.service.IWmArrivalNoticeService;
import com.ktg.mes.wm.service.IWmOutsourceRecptLineService;
import com.ktg.mes.wm.service.IWmOutsourceRecptService;
import com.ktg.mes.wm.service.impl.PreStockVendorReturnService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 来料检验完成的统一编排（PC 与移动端共用）。
 *
 * <p>改造要点：</p>
 * <ul>
 *   <li>来源单、来源行、物料、检验单编号一律以数据库中的检验单为准，前端传入的来源字段只用于一致性校验；</li>
 *   <li>检测量 &gt; 0，合格数 + 不合格数 = 检测量，且累计检测量不超过来源可处理量；</li>
 *   <li>判定结论必须与合格/不合格数量一致（合格量 &gt; 0 为 ACCEPT，合格量为 0 为 REJECT）；</li>
 *   <li>完成使用“状态仍为 PREPARE”的条件更新，重复请求与并发请求只有一次成功；</li>
 *   <li>回写来源行、汇总来源单状态、生成下游采购入库草稿与完成操作同事务。</li>
 * </ul>
 */
@Service
public class QcIqcCompletionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String RESULT_ACCEPT = "ACCEPT";
    private static final String RESULT_REJECT = "REJECT";

    private final IQcIqcService iqcService;
    private final IWmArrivalNoticeService arrivalNoticeService;
    private final IWmArrivalNoticeLineService arrivalNoticeLineService;
    private final IWmOutsourceRecptService outsourceRecptService;
    private final IWmOutsourceRecptLineService outsourceRecptLineService;
    private final PurchaseReceiptLinkageService purchaseReceiptLinkageService;
    private final FormLinkagePolicy formLinkagePolicy;
    private final PostingLockMapper postingLockMapper;
    private final ReceiptGuardMapper guardMapper;
    private final PreStockVendorReturnService preStockVendorReturnService;

    public QcIqcCompletionService(IQcIqcService iqcService,
                                  IWmArrivalNoticeService arrivalNoticeService,
                                  IWmArrivalNoticeLineService arrivalNoticeLineService,
                                  IWmOutsourceRecptService outsourceRecptService,
                                  IWmOutsourceRecptLineService outsourceRecptLineService,
                                  PurchaseReceiptLinkageService purchaseReceiptLinkageService,
                                  FormLinkagePolicy formLinkagePolicy,
                                  PostingLockMapper postingLockMapper,
                                  ReceiptGuardMapper guardMapper,
                                  PreStockVendorReturnService preStockVendorReturnService) {
        this.iqcService = iqcService;
        this.arrivalNoticeService = arrivalNoticeService;
        this.arrivalNoticeLineService = arrivalNoticeLineService;
        this.outsourceRecptService = outsourceRecptService;
        this.outsourceRecptLineService = outsourceRecptLineService;
        this.purchaseReceiptLinkageService = purchaseReceiptLinkageService;
        this.formLinkagePolicy = formLinkagePolicy;
        this.postingLockMapper = postingLockMapper;
        this.guardMapper = guardMapper;
        this.preStockVendorReturnService = preStockVendorReturnService;
    }

    /** 是否为“完成检验”的目标状态：系统同时存在 CONFIRMED（页面确认）与 FINISHED 两种写法。 */
    public static boolean isCompletionStatus(String status) {
        return UserConstants.ORDER_STATUS_CONFIRMED.equals(status)
                || UserConstants.ORDER_STATUS_FINISHED.equals(status);
    }

    /**
     * 保存检验单：草稿只允许改检验数据，完成时走完整校验。
     */
    @Transactional
    public void save(QcIqc request, String operator) {
        if (request == null || request.getIqcId() == null) {
            throw new ServiceException("请先保存检验单");
        }
        Long iqcId = request.getIqcId();
        String persistedStatus = postingLockMapper.iqc(iqcId);
        QcIqc persisted = iqcService.selectQcIqcByIqcId(iqcId);
        if (persisted == null || persistedStatus == null) {
            throw new ServiceException("检验单不存在");
        }
        assertSourceNotTampered(request, persisted);
        restoreSourceSnapshot(request, persisted);

        if (isCompletionStatus(persistedStatus)
                || UserConstants.ORDER_STATUS_CANCELED.equals(persistedStatus)) {
            throw new ServiceException("检验单已完成或已取消，不能重复修改");
        }
        if (isCompletionStatus(request.getStatus())) {
            complete(request, persisted, operator);
            return;
        }
        // 草稿状态：不预填合格，只保存检验输入。
        if (request.getQuantityQualified() == null) {
            request.setQuantityQualified(ZERO);
        }
        if (request.getQuantityUnqualified() == null) {
            request.setQuantityUnqualified(ZERO);
        }
        request.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        request.setUpdateBy(operator);
        iqcService.updateQcIqc(request);
    }

    private void complete(QcIqc request, QcIqc persisted, String operator) {
        BigDecimal quantityCheck = request.getQuantityCheck() != null
                ? request.getQuantityCheck() : persisted.getQuantityRecived();
        BigDecimal qualified = request.getQuantityQualified() == null ? ZERO : request.getQuantityQualified();
        BigDecimal unqualified = request.getQuantityUnqualified() == null ? ZERO : request.getQuantityUnqualified();

        formLinkagePolicy.assertQualityQuantities(quantityCheck, qualified, unqualified);
        BigDecimal received = persisted.getQuantityRecived() == null ? ZERO : persisted.getQuantityRecived();
        if (quantityCheck.compareTo(received) > 0) {
            throw new ServiceException("检测数量不得超过来源可处理数量（本次接收 "
                    + received.stripTrailingZeros().toPlainString() + "）");
        }

        String result = deriveResult(qualified);
        if (StringUtils.isNotEmpty(request.getCheckResult())
                && !result.equalsIgnoreCase(request.getCheckResult())) {
            throw new ServiceException("检测结论与合格/不合格数量不一致：合格量大于0应为 ACCEPT，合格量为0应为 REJECT");
        }

        String sourceType = persisted.getSourceDocType();
        if (StringUtils.isEmpty(sourceType)) {
            // 无来源的手工/历史检验单：只做数量、结论与并发校验，
            // 不回写任何来源单，也不触发下游草稿（不能凭猜测补造下游单）。
        } else if (UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE.equals(sourceType)) {
            completeArrivalNotice(persisted, quantityCheck, qualified, unqualified, operator);
        } else if (UserConstants.SOURCE_DOC_TYPE_OUTSOURCE_RECPT.equals(sourceType)) {
            completeOutsourceRecpt(persisted, qualified, unqualified, operator);
        } else {
            throw new ServiceException("来源单据类型错误，不能完成来料检验");
        }

        int rows = guardMapper.finishIqc(persisted.getIqcId(), UserConstants.ORDER_STATUS_FINISHED,
                quantityCheck, qualified, unqualified, result, operator,
                persisted.getSourceDocType(), persisted.getSourceDocId(), persisted.getSourceDocCode(),
                persisted.getSourceLineId(), persisted.getItemId(), persisted.getVendorId());
        if (rows != 1) {
            throw new ServiceException("检验单已被其他请求完成，请刷新后重试");
        }
        if (UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE.equals(sourceType) && unqualified.signum() > 0) {
            WmArrivalNotice notice = arrivalNoticeService.selectWmArrivalNoticeByNoticeId(persisted.getSourceDocId());
            WmArrivalNoticeLine line = arrivalNoticeLineService.selectWmArrivalNoticeLineByLineId(persisted.getSourceLineId());
            preStockVendorReturnService.create(persisted, notice, line, unqualified, operator);
        }
        request.setStatus(UserConstants.ORDER_STATUS_FINISHED);
        request.setCheckResult(result);
        request.setInspector(operator);
        request.setInspectDate(new java.util.Date());
    }

    private void completeArrivalNotice(QcIqc persisted,
                                       BigDecimal quantityCheck,
                                       BigDecimal qualified,
                                       BigDecimal unqualified,
                                       String operator) {
        if (persisted.getSourceDocId() == null || persisted.getSourceLineId() == null) {
            throw new ServiceException("来料检验缺少必需来源（到货通知单/行），不能完成");
        }
        WmArrivalNoticeLine line = arrivalNoticeLineService.selectWmArrivalNoticeLineByLineId(persisted.getSourceLineId());
        if (line == null) {
            throw new ServiceException("来源到货行不存在，不能完成来料检验");
        }
        if (!persisted.getSourceDocId().equals(line.getNoticeId())) {
            throw new ServiceException("检验单来源行与来源单不匹配，禁止跨单关联");
        }
        if (persisted.getItemId() != null && !persisted.getItemId().equals(line.getItemId())) {
            throw new ServiceException("检验单物料与来源到货行不一致");
        }
        if (!UserConstants.YES.equals(line.getIqcCheck())) {
            throw new ServiceException("该到货行未要求来料检验，不能按检验完成处理");
        }
        BigDecimal arrival = line.getQuantityArrival() == null ? ZERO : line.getQuantityArrival();
        BigDecimal checkedBefore = guardMapper.completedCheckQuantityOnArrivalLine(line.getLineId(), persisted.getIqcId());
        if (checkedBefore == null) {
            checkedBefore = ZERO;
        }
        if (checkedBefore.add(quantityCheck).compareTo(arrival) > 0) {
            throw new ServiceException("累计检测数量超过来源可处理数量：已检 "
                    + checkedBefore.stripTrailingZeros().toPlainString() + "，到货 "
                    + arrival.stripTrailingZeros().toPlainString());
        }

        line.setIqcCode(persisted.getIqcCode());
        line.setIqcId(persisted.getIqcId());
        line.setQuantityQuanlified(qualified);
        arrivalNoticeLineService.updateWmArrivalNoticeLine(line);

        // 汇总到货通知状态：所有需检行都有检验结果才进入已放行
        arrivalNoticeService.updateStatus(persisted.getSourceDocId());

        // 下游采购入库草稿：同一到货通知只生成一张；全 NG 时不会产生空草稿
        WmArrivalNotice notice = arrivalNoticeService.selectWmArrivalNoticeByNoticeId(persisted.getSourceDocId());
        WmArrivalNoticeLine query = new WmArrivalNoticeLine();
        query.setNoticeId(persisted.getSourceDocId());
        List<WmArrivalNoticeLine> noticeLines = arrivalNoticeLineService.selectWmArrivalNoticeLineList(query);
        purchaseReceiptLinkageService.createDraftWhenReady(notice, noticeLines, operator);
    }

    private void completeOutsourceRecpt(QcIqc persisted,
                                        BigDecimal qualified,
                                        BigDecimal unqualified,
                                        String operator) {
        if (persisted.getSourceDocId() == null || persisted.getSourceLineId() == null) {
            throw new ServiceException("外协入库检验缺少必需来源，不能完成");
        }
        WmOutsourceRecptLine line = outsourceRecptLineService
                .selectWmOutsourceRecptLineByLineId(persisted.getSourceLineId());
        WmOutsourceRecpt recpt = outsourceRecptService.selectWmOutsourceRecptByRecptId(persisted.getSourceDocId());
        if (line == null || recpt == null) {
            throw new ServiceException("外协入库来源单或来源行不存在");
        }
        if (persisted.getItemId() != null && !persisted.getItemId().equals(line.getItemId())) {
            throw new ServiceException("检验单物料与外协入库行不一致");
        }
        if (qualified.compareTo(ZERO) > 0) {
            line.setIqcCode(persisted.getIqcCode());
            line.setIqcId(persisted.getIqcId());
            line.setQuantityRecived(qualified);
            line.setQualityStatus(UserConstants.QUALITY_STATUS_PASS);
            outsourceRecptLineService.updateWmOutsourceRecptLine(line);
        }
        if (unqualified.compareTo(ZERO) > 0) {
            WmOutsourceRecptLine unqualifiedLine = new WmOutsourceRecptLine();
            BeanUtils.copyBeanProp(unqualifiedLine, line);
            unqualifiedLine.setLineId(null);
            unqualifiedLine.setIqcCode(persisted.getIqcCode());
            unqualifiedLine.setIqcId(persisted.getIqcId());
            unqualifiedLine.setQuantityRecived(unqualified);
            unqualifiedLine.setQualityStatus(UserConstants.QUALITY_STATUS_NOTGOOD);
            outsourceRecptLineService.insertWmOutsourceRecptLine(unqualifiedLine);
        }
        recpt.setStatus(UserConstants.OUTSOURCE_RECPT_STATUS_UNSTOCK);
        outsourceRecptService.updateWmOutsourceRecpt(recpt);
    }

    /** 判定结论必须与数量一致：有合格量才能放行。 */
    public static String deriveResult(BigDecimal qualified) {
        return qualified != null && qualified.compareTo(ZERO) > 0 ? RESULT_ACCEPT : RESULT_REJECT;
    }

    /** 前端不得改写来源单/来源行/物料：一旦与库中记录不一致就拒绝。 */
    private void assertSourceNotTampered(QcIqc request, QcIqc persisted) {
        assertSame("来源单据", request.getSourceDocId(), persisted.getSourceDocId());
        assertSame("来源单据行", request.getSourceLineId(), persisted.getSourceLineId());
        assertSame("来源单据类型", request.getSourceDocType(), persisted.getSourceDocType());
        assertSame("物料", request.getItemId(), persisted.getItemId());
    }

    private void assertSame(String what, Object requested, Object persisted) {
        if (requested == null) {
            return;
        }
        if (persisted == null || !requested.equals(persisted)) {
            throw new ServiceException("检验单的" + what + "不允许通过普通接口修改");
        }
    }

    /**
     * 用库中的来源快照覆盖请求对象，保证回写与下游建单使用的是可信来源。
     */
    private void restoreSourceSnapshot(QcIqc request, QcIqc persisted) {
        request.setIqcCode(persisted.getIqcCode());
        request.setSourceDocType(persisted.getSourceDocType());
        request.setSourceDocId(persisted.getSourceDocId());
        request.setSourceDocCode(persisted.getSourceDocCode());
        request.setSourceLineId(persisted.getSourceLineId());
        request.setItemId(persisted.getItemId());
        request.setItemCode(persisted.getItemCode());
        request.setItemName(persisted.getItemName());
        request.setSpecification(persisted.getSpecification());
        request.setUnitOfMeasure(persisted.getUnitOfMeasure());
        request.setUnitName(persisted.getUnitName());
        request.setVendorId(persisted.getVendorId());
        request.setVendorCode(persisted.getVendorCode());
        request.setVendorName(persisted.getVendorName());
        request.setVendorNick(persisted.getVendorNick());
        request.setTemplateId(persisted.getTemplateId());
        // 本次接收数量属于来源快照，一律以库中记录为准，避免通过编辑放大可检数量。
        request.setQuantityRecived(persisted.getQuantityRecived());
    }
}
