package com.ktg.mes;

import com.ktg.common.constant.UserConstants;
import com.ktg.mes.md.domain.MdItem;
import com.ktg.mes.md.domain.MdItemBatchConfig;
import com.ktg.mes.md.service.IMdItemBatchConfigService;
import com.ktg.mes.md.service.IMdItemService;
import com.ktg.mes.md.domain.MdWorkstation;
import com.ktg.mes.md.mapper.MdWorkstationMapper;
import com.ktg.mes.linkage.MenuLinkageNoticeService;
import com.ktg.mes.linkage.PurchaseReceiptLinkageService;
import com.ktg.mes.pro.domain.*;
import com.ktg.mes.pro.mapper.*;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.mapper.*;
import com.ktg.mes.wm.service.IWmItemRecptService;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.mes.wm.service.impl.WmBatchServiceImpl;
import com.ktg.mes.wm.service.impl.WmProductProduceServiceImpl;
import com.ktg.mes.wm.utils.WmBarCodeUtil;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WmBatchPilotScopeTest {
    private final WmBatchServiceImpl service = new WmBatchServiceImpl();
    private final WmBatchMapper batches = mock(WmBatchMapper.class);
    private final MdItemBatchConfig config = new MdItemBatchConfig();
    private final MdItem item = new MdItem();
    private final Map<String, WmBatch> stored = new LinkedHashMap<>();
    private WmBatch historical;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "wmBatchMapper", batches);
        ReflectionTestUtils.setField(service, "barcodeUtil", mock(WmBarCodeUtil.class));
        AutoCodeUtil codes = mock(AutoCodeUtil.class);
        ReflectionTestUtils.setField(service, "autoCodeUtil", codes);
        when(codes.genSerialCode(UserConstants.BATCH_CODE, "")).thenAnswer(invocation -> "NEW-" + stored.size());
        IMdItemService items = mock(IMdItemService.class);
        ReflectionTestUtils.setField(service, "mdItemService", items);
        item.setBatchFlag(UserConstants.YES);
        when(items.selectMdItemById(any())).thenReturn(item);
        IMdItemBatchConfigService configs = mock(IMdItemBatchConfigService.class);
        ReflectionTestUtils.setField(service, "mdItemBatchConfigService", configs);
        when(configs.getMdItemBatchConfigByItemId(any())).thenReturn(config);
        config.setProduceDateFlag(UserConstants.NO); config.setRecptDateFlag(UserConstants.NO);
        config.setExpireDateFlag(UserConstants.NO); config.setVendorFlag(UserConstants.NO);
        config.setClientFlag(UserConstants.NO); config.setPoCodeFlag(UserConstants.NO);
        config.setCoCodeFlag(UserConstants.NO); config.setWorkorderFlag(UserConstants.NO);
        config.setTaskFlag(UserConstants.NO); config.setWorkstationFlag(UserConstants.NO);
        config.setToolFlag(UserConstants.NO); config.setMoldFlag(UserConstants.NO);
        config.setLotNumberFlag(UserConstants.NO); config.setQualityStatusFlag(UserConstants.NO);
        historical = input(10L); historical.setBatchId(1L); historical.setBatchCode("HISTORICAL");
        stored.put(key(historical), historical);
        // 模拟全维度关闭时实际 mapper 按物料 + lot_number（包含 null）查询的持久化边界。
        when(batches.getBatchCodeByParams(any())).thenAnswer(invocation -> stored.get(key(invocation.getArgument(0))));
        when(batches.insertWmBatch(any())).thenAnswer(invocation -> {
            WmBatch batch = invocation.getArgument(0);
            batch.setBatchId((long) stored.size() + 1); stored.put(key(batch), batch); return 1;
        });
    }

    @Test
    void purchasePoScopeCannotReuseHistoricalBatchWhenAllDimensionsAreDisabled() {
        WmBatch purchase = input(10L); purchase.setPoCode("PILOT-DABC123");
        WmBatch created = service.getOrGenerateBatchCode(purchase);
        assertNotEquals("HISTORICAL", created.getBatchCode());
        assertEquals("PILOT-DABC123", created.getLotNumber());
        assertNull(created.getPoCode());
        assertSame(historical, stored.get("10|null"));
    }

    @Test
    void productionLotScopeCannotReuseHistoricalBatchWhenAllDimensionsAreDisabled() {
        WmBatch production = input(10L); production.setLotNumber("PILOT-DABC123");
        WmBatch created = service.getOrGenerateBatchCode(production);
        assertNotEquals("HISTORICAL", created.getBatchCode());
        assertEquals("PILOT-DABC123", created.getLotNumber());
    }

    @Test
    void sameRunAndItemReuseOneBatchAcrossPurchaseAndProductionInputs() {
        WmBatch purchase = input(10L); purchase.setPoCode("PILOT-DABC123");
        WmBatch first = service.getOrGenerateBatchCode(purchase);
        WmBatch production = input(10L); production.setLotNumber("PILOT-DABC123");
        WmBatch repeated = service.getOrGenerateBatchCode(production);
        assertSame(first, repeated);
        assertEquals("PILOT-DABC123", repeated.getLotNumber());
        assertEquals(2, stored.size());
    }

    @Test
    void differentRunsAndItemsRemainSeparateFromEachOtherAndHistory() {
        WmBatch first = input(10L); first.setPoCode("PILOT-DRUN1");
        WmBatch second = input(10L); second.setPoCode("PILOT-DRUN2");
        WmBatch anotherItem = input(11L); anotherItem.setLotNumber("PILOT-DRUN1");
        Set<String> codes = new HashSet<>(); codes.add(historical.getBatchCode());
        codes.add(service.getOrGenerateBatchCode(first).getBatchCode());
        codes.add(service.getOrGenerateBatchCode(second).getBatchCode());
        codes.add(service.getOrGenerateBatchCode(anotherItem).getBatchCode());
        assertEquals(4, codes.size());
    }

    @Test
    void ordinaryValuesKeepExistingConfiguredBehavior() {
        WmBatch ordinary = input(10L); ordinary.setPoCode("PO-ORDINARY");
        ordinary.setLotNumber("LOT-ORDINARY"); ordinary.setWorkorderCode("WORKORDER-ORDINARY");
        assertSame(historical, service.getOrGenerateBatchCode(ordinary));
        assertNull(ordinary.getPoCode()); assertNull(ordinary.getLotNumber());
        assertNull(ordinary.getWorkorderCode());
    }

    @Test
    void malformedReservedScopeIsRejectedInsteadOfFallingBackToHistory() {
        for (String invalid : Arrays.asList("PILOT-", "PILOT-abc", "PILOT-ABC-123", "PILOT-ABC 123", "PILOT-ABC@123")) {
            WmBatch purchase = input(10L); purchase.setPoCode(invalid);
            assertThrows(IllegalArgumentException.class, () -> service.getOrGenerateBatchCode(purchase), invalid);
            WmBatch production = input(10L); production.setLotNumber(invalid);
            assertThrows(IllegalArgumentException.class, () -> service.getOrGenerateBatchCode(production), invalid);
        }
        verify(batches, never()).getBatchCodeByParams(any());
    }

    @Test
    void conflictingPurchaseAndProductionScopesAreRejected() {
        WmBatch conflicting = input(10L); conflicting.setPoCode("PILOT-DRUN1");
        conflicting.setLotNumber("PILOT-DRUN2");
        assertThrows(IllegalArgumentException.class, () -> service.getOrGenerateBatchCode(conflicting));
    }

    @Test
    void pilotScopeDoesNotBypassRequiredConfiguredFields() {
        config.setVendorFlag(UserConstants.YES);
        WmBatch missingVendor = input(10L); missingVendor.setPoCode("PILOT-DRUN1");
        assertThrows(IllegalArgumentException.class, () -> service.getOrGenerateBatchCode(missingVendor));
        verify(batches, never()).getBatchCodeByParams(any());
    }

    @Test
    void itemWithoutBatchManagementStillReturnsNull() {
        item.setBatchFlag(UserConstants.NO);
        WmBatch unmanaged = input(10L); unmanaged.setLotNumber("PILOT-DRUN1");
        assertNull(service.getOrGenerateBatchCode(unmanaged));
        verify(batches, never()).getBatchCodeByParams(any());
    }

    @Test
    void scopeLengthFitsThePurchaseOrderField() {
        WmBatch tooLong = input(10L);
        tooLong.setPoCode("PILOT-" + String.join("", Collections.nCopies(59, "A")));
        assertThrows(IllegalArgumentException.class, () -> service.getOrGenerateBatchCode(tooLong));
        WmBatch boundary = input(10L);
        boundary.setPoCode("PILOT-" + String.join("", Collections.nCopies(58, "A")));
        assertEquals(64, service.getOrGenerateBatchCode(boundary).getLotNumber().length());
    }

    @Test
    void arrivalNoticeScopeReachesBatchThroughAutomaticPurchaseReceipt() {
        IWmItemRecptService receipts = mock(IWmItemRecptService.class);
        IWmItemRecptLineService receiptLines = mock(IWmItemRecptLineService.class);
        PurchaseReceiptLinkageService linkage = new PurchaseReceiptLinkageService(receipts, receiptLines, service,
                mock(AutoCodeUtil.class), mock(MenuLinkageNoticeService.class), mock(IQcIqcService.class));
        when(receipts.selectWmItemRecptList(any())).thenReturn(Collections.emptyList());
        WmArrivalNotice notice = new WmArrivalNotice(); notice.setNoticeId(1L);
        notice.setNoticeCode("ARRIVAL-1"); notice.setPoCode("PILOT-DRUN1");
        WmArrivalNoticeLine source = new WmArrivalNoticeLine(); source.setLineId(2L);
        source.setItemId(10L); source.setItemCode("ITEM-10"); source.setQuantityArrival(BigDecimal.TEN);
        source.setIqcCheck(UserConstants.NO);
        WmItemRecpt receipt = linkage.createDraftWhenReady(notice, Collections.singletonList(source), "tester");
        ArgumentCaptor<WmItemRecptLine> capture = ArgumentCaptor.forClass(WmItemRecptLine.class);
        verify(receiptLines).insertWmItemRecptLine(capture.capture());
        assertEquals("PILOT-DRUN1", receipt.getPoCode());
        assertNotEquals("HISTORICAL", capture.getValue().getBatchCode());
        assertEquals(capture.getValue().getBatchCode(), stored.get("10|PILOT-DRUN1").getBatchCode());
    }

    @Test
    void feedbackScopeReachesBatchThroughProductProduceGeneration() {
        WmProductProduceServiceImpl produce = new WmProductProduceServiceImpl();
        ReflectionTestUtils.setField(produce, "wmBatchService", service);
        ProWorkorderMapper workorders = mock(ProWorkorderMapper.class);
        ReflectionTestUtils.setField(produce, "proWorkorderMapper", workorders);
        ProWorkorder workorder = new ProWorkorder(); workorder.setWorkorderCode("WORKORDER-1");
        when(workorders.selectProWorkorderByWorkorderId(1L)).thenReturn(workorder);
        MdWorkstationMapper workstations = mock(MdWorkstationMapper.class);
        ReflectionTestUtils.setField(produce, "mdWorkstationMapper", workstations);
        MdWorkstation workstation = new MdWorkstation(); workstation.setProcessId(3L);
        when(workstations.selectMdWorkstationByWorkstationId(2L)).thenReturn(workstation);
        ProProcessMapper processes = mock(ProProcessMapper.class);
        ReflectionTestUtils.setField(produce, "proProcessMapper", processes);
        when(processes.selectProProcessByProcessId(3L)).thenReturn(new ProProcess());
        ProTaskMapper tasks = mock(ProTaskMapper.class);
        ReflectionTestUtils.setField(produce, "proTaskMapper", tasks);
        when(tasks.selectProTaskByTaskId(4L)).thenReturn(new ProTask());
        WmProductProduceMapper headers = mock(WmProductProduceMapper.class);
        ReflectionTestUtils.setField(produce, "wmProductProduceMapper", headers);
        when(headers.insertWmProductProduce(any())).thenAnswer(invocation -> {
            invocation.<WmProductProduce>getArgument(0).setRecordId(5L); return 1;
        });
        WmProductProduceLineMapper lines = mock(WmProductProduceLineMapper.class);
        ReflectionTestUtils.setField(produce, "wmProductProduceLineMapper", lines);
        ReflectionTestUtils.setField(produce, "wmWarehouseMapper", mock(WmWarehouseMapper.class));
        ReflectionTestUtils.setField(produce, "wmStorageLocationMapper", mock(WmStorageLocationMapper.class));
        ReflectionTestUtils.setField(produce, "wmStorageAreaMapper", mock(WmStorageAreaMapper.class));
        ProFeedback feedback = new ProFeedback(); feedback.setWorkorderId(1L); feedback.setWorkstationId(2L);
        feedback.setTaskId(4L); feedback.setItemId(10L); feedback.setItemCode("ITEM-10");
        feedback.setQuantityFeedback(BigDecimal.TEN); feedback.setIsCheck(UserConstants.YES);
        feedback.setLotNumber("PILOT-DRUN1");
        produce.generateProductProduce(feedback);
        ArgumentCaptor<WmProductProduceLine> capture = ArgumentCaptor.forClass(WmProductProduceLine.class);
        verify(lines).insertWmProductProduceLine(capture.capture());
        assertEquals("PILOT-DRUN1", capture.getValue().getLotNumber());
        assertNotEquals("HISTORICAL", capture.getValue().getBatchCode());
        assertEquals(capture.getValue().getBatchCode(), stored.get("10|PILOT-DRUN1").getBatchCode());
    }

    private WmBatch input(Long itemId) {
        WmBatch batch = new WmBatch(); batch.setItemId(itemId);
        batch.setItemCode("ITEM-" + itemId); batch.setItemName("测温枪整机"); return batch;
    }

    private String key(WmBatch batch) { return batch.getItemId() + "|" + batch.getLotNumber(); }
}
