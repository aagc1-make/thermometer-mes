package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 表单联动的统一放行规则。库存执行入口必须在服务端调用这些规则，
 * 前端按钮状态只用于提示，不能替代服务端校验。
 */
@Component
public class FormLinkagePolicy {

    public void assertPurchaseReceiptExecutable(WmItemRecpt receipt,
                                                List<WmArrivalNoticeLine> sourceLines) {
        if (receipt == null) {
            throw new ServiceException("采购入库单不存在");
        }
        if (!UserConstants.ORDER_STATUS_APPROVED.equals(receipt.getStatus())) {
            throw new ServiceException("采购入库单不是待执行状态，禁止重复或越级入库");
        }
        if (sourceLines == null) {
            return;
        }
        for (WmArrivalNoticeLine line : sourceLines) {
            if (UserConstants.YES.equals(line.getIqcCheck())
                    && (line.getIqcId() == null
                    || line.getQuantityQuanlified() == null
                    || line.getQuantityQuanlified().compareTo(BigDecimal.ZERO) <= 0)) {
                throw new ServiceException("来料检验尚未放行，不能执行采购入库");
            }
        }
    }

    public void assertSalesExecutable(WmProductSales sales,
                                      List<WmProductSalesLine> lines) {
        if (sales == null) {
            throw new ServiceException("销售出库单不存在");
        }
        if (!UserConstants.PRODUCT_SALES_STATUS_UNEXECUTE.equals(sales.getStatus())) {
            throw new ServiceException("销售出库单不是待执行状态，禁止重复或越级出库");
        }
        if (lines == null) {
            return;
        }
        for (WmProductSalesLine line : lines) {
            if (UserConstants.YES.equals(line.getOqcCheck())
                    && !UserConstants.QUALITY_STATUS_PASS.equals(line.getQualityStatus())) {
                throw new ServiceException("出货检验尚未合格，不能执行销售出库");
            }
        }
    }

    public void assertQualityQuantities(BigDecimal checked,
                                        BigDecimal qualified,
                                        BigDecimal unqualified) {
        if (checked == null || qualified == null || unqualified == null
                || checked.compareTo(BigDecimal.ZERO) <= 0
                || qualified.compareTo(BigDecimal.ZERO) < 0
                || unqualified.compareTo(BigDecimal.ZERO) < 0
                || checked.compareTo(qualified.add(unqualified)) != 0) {
            throw new ServiceException("检测数量必须等于合格数量与不合格数量之和");
        }
    }

    public void assertQualityCompletionAllowed(String persistedStatus, String requestedStatus) {
        if (UserConstants.ORDER_STATUS_FINISHED.equals(requestedStatus)
                && UserConstants.ORDER_STATUS_FINISHED.equals(persistedStatus)) {
            throw new ServiceException("检验单已完成，禁止重复执行联动");
        }
    }

    public void assertFeedbackExecutable(String status) {
        if (UserConstants.FEEDBACK_STATUS_UNCHECK.equals(status)
                || UserConstants.FEEDBACK_STATUS_FINISHED.equals(status)
                || UserConstants.FEEDBACK_STATUS_CANCELED.equals(status)) {
            throw new ServiceException("报工单已进入待检、完成或取消状态，禁止重复执行");
        }
    }
}
