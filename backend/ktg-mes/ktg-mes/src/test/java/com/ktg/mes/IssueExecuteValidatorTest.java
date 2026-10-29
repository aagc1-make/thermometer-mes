package com.ktg.mes;

import com.ktg.mes.wm.domain.WmIssueDetail;
import com.ktg.mes.wm.domain.WmIssueLine;
import com.ktg.mes.wm.service.IWmIssueDetailService;
import com.ktg.mes.wm.service.impl.IssueExecuteValidator;
import com.ktg.mes.wm.mapper.WmMaterialStockMapper;
import com.ktg.mes.wm.domain.WmMaterialStock;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IssueExecuteValidatorTest {
    @Test public void incompleteAndOverpickedAreRejectedBeforeStockLookup() {
        IssueExecuteValidator validator = new IssueExecuteValidator();
        IWmIssueDetailService details = mock(IWmIssueDetailService.class);
        WmMaterialStockMapper stocks = mock(WmMaterialStockMapper.class);
        ReflectionTestUtils.setField(validator, "wmIssueDetailService", details);
        ReflectionTestUtils.setField(validator, "wmMaterialStockMapper", stocks);
        WmIssueLine line = new WmIssueLine(); line.setIssueId(1L); line.setLineId(2L); line.setItemId(3L); line.setQuantityIssued(new BigDecimal("5"));
        when(details.selectWmIssueDetailList(any(WmIssueDetail.class))).thenReturn(java.util.Collections.emptyList());
        assertTrue(validator.validate(Arrays.asList(line)).contains("尚未拣货"));
        WmIssueDetail d = new WmIssueDetail(); d.setItemId(3L); d.setMaterialStockId("7"); d.setQuantity(new BigDecimal("4"));
        when(details.selectWmIssueDetailList(any(WmIssueDetail.class))).thenReturn(Arrays.asList(d));
        assertTrue(validator.validate(Arrays.asList(line)).contains("尚差"));
        d.setQuantity(new BigDecimal("6"));
        assertTrue(validator.validate(Arrays.asList(line)).contains("超出"));
        verifyNoInteractions(stocks);
    }

    @Test public void nonexistentStockAndTamperedLocationAreRejected() {
        IssueExecuteValidator validator = new IssueExecuteValidator();
        IWmIssueDetailService details = mock(IWmIssueDetailService.class);
        WmMaterialStockMapper stocks = mock(WmMaterialStockMapper.class);
        ReflectionTestUtils.setField(validator, "wmIssueDetailService", details);
        ReflectionTestUtils.setField(validator, "wmMaterialStockMapper", stocks);
        WmIssueLine line = new WmIssueLine(); line.setIssueId(1L); line.setLineId(2L); line.setItemId(3L); line.setQuantityIssued(BigDecimal.ONE);
        WmIssueDetail d = new WmIssueDetail(); d.setItemId(3L); d.setMaterialStockId("7"); d.setQuantity(BigDecimal.ONE); d.setBatchId(8L); d.setBatchCode("B"); d.setWarehouseId(9L); d.setLocationId(10L); d.setAreaId(11L);
        when(details.selectWmIssueDetailList(any(WmIssueDetail.class))).thenReturn(Arrays.asList(d));
        assertTrue(validator.validate(Arrays.asList(line)).contains("不存在"));
        WmMaterialStock stock = new WmMaterialStock(); stock.setItemId(3L); stock.setBatchId(8L); stock.setBatchCode("B"); stock.setWarehouseId(9L); stock.setLocationId(10L); stock.setAreaId(12L); stock.setQuantityOnhand(BigDecimal.TEN);
        when(stocks.lockMaterialStockById(7L)).thenReturn(stock);
        assertTrue(validator.validate(Arrays.asList(line)).contains("不一致"));
    }
    @Test public void missingStockMustBeRejected() {
        IssueExecuteValidator validator = new IssueExecuteValidator();
        IWmIssueDetailService details = mock(IWmIssueDetailService.class);
        ReflectionTestUtils.setField(validator, "wmIssueDetailService", details);
        WmIssueLine line = new WmIssueLine();
        line.setIssueId(1L); line.setLineId(2L); line.setItemId(3L);
        line.setQuantityIssued(BigDecimal.ONE);
        WmIssueDetail detail = new WmIssueDetail();
        detail.setItemId(3L); detail.setQuantity(BigDecimal.ONE);
        when(details.selectWmIssueDetailList(any(WmIssueDetail.class))).thenReturn(Arrays.asList(detail));
        assertNotNull("未关联库存必须拒绝", validator.validate(Arrays.asList(line)));
    }
    @Test public void sameStockIsSummedAcrossLinesAndBatchIsChecked() {
        IssueExecuteValidator validator = new IssueExecuteValidator();
        IWmIssueDetailService details = mock(IWmIssueDetailService.class);
        WmMaterialStockMapper stocks = mock(WmMaterialStockMapper.class);
        ReflectionTestUtils.setField(validator, "wmIssueDetailService", details);
        ReflectionTestUtils.setField(validator, "wmMaterialStockMapper", stocks);
        WmIssueLine first = new WmIssueLine(); first.setIssueId(1L); first.setLineId(1L); first.setItemId(3L); first.setQuantityIssued(new BigDecimal("6"));
        WmIssueLine second = new WmIssueLine(); second.setIssueId(1L); second.setLineId(2L); second.setItemId(3L); second.setQuantityIssued(new BigDecimal("6"));
        WmIssueDetail detail = new WmIssueDetail(); detail.setItemId(3L); detail.setMaterialStockId("7"); detail.setBatchId(8L); detail.setBatchCode("B"); detail.setWarehouseId(9L); detail.setLocationId(10L); detail.setAreaId(11L); detail.setQuantity(new BigDecimal("6"));
        WmMaterialStock stock = new WmMaterialStock(); stock.setItemId(3L); stock.setBatchId(8L); stock.setBatchCode("B"); stock.setWarehouseId(9L); stock.setLocationId(10L); stock.setAreaId(11L); stock.setQuantityOnhand(new BigDecimal("10"));
        when(details.selectWmIssueDetailList(any(WmIssueDetail.class))).thenReturn(Arrays.asList(detail));
        when(stocks.lockMaterialStockById(7L)).thenReturn(stock);
        assertTrue(validator.validate(Arrays.asList(first, second)).contains("库存不足"));
        stock.setQuantityOnhand(new BigDecimal("20")); stock.setBatchCode("OTHER");
        assertTrue(validator.validate(Arrays.asList(first)).contains("不一致"));
        stock.setBatchCode("B");
        assertNull(validator.validate(Arrays.asList(first, second)));
    }
}
