package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmProductSales;
import com.ktg.mes.wm.domain.WmProductSalesLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormLinkagePolicyTest {

    private final FormLinkagePolicy policy = new FormLinkagePolicy();

    @Test
    void rejectsPurchaseReceiptUnlessItIsReadyToExecute() {
        WmItemRecpt receipt = new WmItemRecpt();
        receipt.setStatus(UserConstants.ORDER_STATUS_PREPARE);

        assertThrows(ServiceException.class,
                () -> policy.assertPurchaseReceiptExecutable(receipt, Collections.emptyList()));
    }

    @Test
    void rejectsPurchaseReceiptWhenRequiredIqcHasNotPassed() {
        WmItemRecpt receipt = new WmItemRecpt();
        receipt.setStatus(UserConstants.ORDER_STATUS_APPROVED);
        WmArrivalNoticeLine line = new WmArrivalNoticeLine();
        line.setIqcCheck(UserConstants.YES);
        line.setQuantityArrival(new BigDecimal("10"));
        line.setQuantityQuanlified(null);

        assertThrows(ServiceException.class,
                () -> policy.assertPurchaseReceiptExecutable(receipt, Collections.singletonList(line)));
    }

    @Test
    void allowsPurchaseReceiptAfterRequiredIqcHasQualifiedQuantity() {
        WmItemRecpt receipt = new WmItemRecpt();
        receipt.setStatus(UserConstants.ORDER_STATUS_APPROVED);
        WmArrivalNoticeLine line = new WmArrivalNoticeLine();
        line.setIqcCheck(UserConstants.YES);
        line.setIqcId(99L);
        line.setQuantityArrival(new BigDecimal("10"));
        line.setQuantityQuanlified(new BigDecimal("8"));

        assertDoesNotThrow(
                () -> policy.assertPurchaseReceiptExecutable(receipt, Collections.singletonList(line)));
    }

    @Test
    void rejectsSalesExecutionUntilRequiredOqcPasses() {
        WmProductSales sales = new WmProductSales();
        sales.setStatus(UserConstants.PRODUCT_SALES_STATUS_UNEXECUTE);
        WmProductSalesLine line = new WmProductSalesLine();
        line.setOqcCheck(UserConstants.YES);
        line.setQualityStatus(UserConstants.QUALITY_STATUS_NOTTEST);

        assertThrows(ServiceException.class,
                () -> policy.assertSalesExecutable(sales, Collections.singletonList(line)));
    }

    @Test
    void rejectsRepeatedSalesExecution() {
        WmProductSales sales = new WmProductSales();
        sales.setStatus(UserConstants.PRODUCT_SALES_STATUS_FINISHED);

        assertThrows(ServiceException.class,
                () -> policy.assertSalesExecutable(sales, Collections.emptyList()));
    }

    @Test
    void acceptsOnlyConservedQualityQuantities() {
        assertDoesNotThrow(() -> policy.assertQualityQuantities(
                new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("2")));
        assertThrows(ServiceException.class, () -> policy.assertQualityQuantities(
                new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("1")));
    }

    @Test
    void rejectsRepeatedQualityCompletion() {
        assertThrows(ServiceException.class, () -> policy.assertQualityCompletionAllowed(
                UserConstants.ORDER_STATUS_FINISHED, UserConstants.ORDER_STATUS_FINISHED));
        assertDoesNotThrow(() -> policy.assertQualityCompletionAllowed(
                UserConstants.ORDER_STATUS_PREPARE, UserConstants.ORDER_STATUS_FINISHED));
    }

    @Test
    void rejectsFeedbackThatIsAlreadyWaitingForInspection() {
        assertThrows(ServiceException.class,
                () -> policy.assertFeedbackExecutable(UserConstants.FEEDBACK_STATUS_UNCHECK));
        assertDoesNotThrow(
                () -> policy.assertFeedbackExecutable(UserConstants.FEEDBACK_STATUS_PREPARE));
    }
}
