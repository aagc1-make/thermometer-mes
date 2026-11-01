package com.ktg.mes;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.md.service.IMdProductBomService;
import com.ktg.mes.pro.controller.ProWorkorderController;
import com.ktg.mes.pro.controller.vo.SubWorkorderBatchRequest;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.domain.ProWorkorderBom;
import com.ktg.mes.pro.service.IProWorkorderBomService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.mapper.ReceiptGuardMapper;
import com.ktg.mes.wm.utils.WmBarCodeUtil;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProWorkorderSubBatchInheritanceTest {
    private final ProWorkorderController controller = new ProWorkorderController() {
        @Override public String getUsername() { return "tester"; }
    };
    private final IProWorkorderService workorders = mock(IProWorkorderService.class);
    private final ReceiptGuardMapper guards = mock(ReceiptGuardMapper.class);
    private final ProWorkorder parent = new ProWorkorder();
    private final SubWorkorderBatchRequest request = new SubWorkorderBatchRequest();
    private final Map<Long, ProWorkorder> inserted = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "proWorkorderService", workorders);
        ReflectionTestUtils.setField(controller, "receiptGuardMapper", guards);
        ReflectionTestUtils.setField(controller, "wmBarCodeUtil", mock(WmBarCodeUtil.class));
        IProWorkorderBomService boms = mock(IProWorkorderBomService.class);
        ReflectionTestUtils.setField(controller, "proWorkorderBomService", boms);
        IMdProductBomService productBoms = mock(IMdProductBomService.class);
        ReflectionTestUtils.setField(controller, "mdProductBomService", productBoms);
        when(productBoms.selectMdProductBomList(any())).thenReturn(Collections.emptyList());
        AutoCodeUtil codes = mock(AutoCodeUtil.class);
        ReflectionTestUtils.setField(controller, "autoCodeUtil", codes);
        when(codes.genSerialCode(UserConstants.WORKORDER_CODE, "")).thenReturn("CHILD-001", "CHILD-002");

        parent.setWorkorderId(1L); parent.setWorkorderCode("PARENT-001"); parent.setAncestors("0");
        parent.setStatus(UserConstants.ORDER_STATUS_CONFIRMED); parent.setWorkorderType("SELF");
        parent.setRequestDate(new Date(1893456000000L));
        parent.setClientId(70L); parent.setClientCode("CLIENT-70"); parent.setClientName("测试客户");
        when(workorders.selectProWorkorderByWorkorderId(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return id.equals(1L) ? parent : inserted.get(id);
        });
        when(workorders.insertProWorkorder(any())).thenAnswer(invocation -> {
            ProWorkorder child = invocation.getArgument(0);
            child.setWorkorderId(1000L + inserted.size());
            inserted.put(child.getWorkorderId(), child);
            return 1;
        });
        ProWorkorderBom first = bom(11L, 100L);
        ProWorkorderBom second = bom(12L, 101L);
        when(boms.selectProWorkorderBomList(any())).thenReturn(Arrays.asList(first, second));
        request.setParentId(1L); request.setBomLineIds(Arrays.asList(11L, 12L));
    }

    @Test
    void generatedChildrenInheritParentDeliveryDateTypeAndClient() {
        assertEquals(200, controller.addSubBatch(request).get("code"));
        ArgumentCaptor<ProWorkorder> capture = ArgumentCaptor.forClass(ProWorkorder.class);
        verify(workorders, times(2)).insertProWorkorder(capture.capture());
        List<ProWorkorder> children = capture.getAllValues();
        for (ProWorkorder child : children) {
            assertEquals(new Date(1893456000000L), child.getRequestDate());
            assertEquals("SELF", child.getWorkorderType());
            assertEquals(70L, child.getClientId());
            assertEquals("CLIENT-70", child.getClientCode());
            assertEquals("测试客户", child.getClientName());
            assertEquals(1L, child.getParentId());
            assertEquals("0,1", child.getAncestors());
            assertEquals("SUB_WORKORDER", child.getOrderSource());
            assertEquals("PARENT-001", child.getSourceCode());
            assertEquals(UserConstants.ORDER_STATUS_PREPARE, child.getStatus());
        }
        assertEquals(100L, children.get(0).getProductId());
        assertEquals(101L, children.get(1).getProductId());
        assertEquals("AUTO:SUB_WORKORDER:1:11", children.get(0).getAttr1());
        assertEquals("AUTO:SUB_WORKORDER:1:12", children.get(1).getAttr1());
    }

    @Test
    @SuppressWarnings("unchecked")
    void retrySkipsAlreadyCreatedSourceBom() {
        request.setBomLineIds(Collections.singletonList(11L));
        when(guards.countSubWorkorder("AUTO:SUB_WORKORDER:1:11")).thenReturn(1);
        AjaxResult response = controller.addSubBatch(request);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals(0, data.get("createdCount"));
        assertEquals(1, data.get("skippedCount"));
        verify(workorders, never()).insertProWorkorder(any());
    }

    @Test
    void laterChildFailureRollsBackWholeBatch() {
        doAnswer(invocation -> {
            ProWorkorder child = invocation.getArgument(0);
            if (!inserted.isEmpty()) throw new ServiceException("第二张子工单保存失败");
            child.setWorkorderId(1000L); inserted.put(1000L, child); return 1;
        }).when(workorders).insertProWorkorder(any());
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        TransactionStatus transaction = mock(TransactionStatus.class);
        when(transactions.getTransaction(any())).thenReturn(transaction);
        ProxyFactory factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        ProWorkorderController transactional = (ProWorkorderController) factory.getProxy();
        assertThrows(ServiceException.class, () -> transactional.addSubBatch(request));
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
    }

    private ProWorkorderBom bom(Long lineId, Long itemId) {
        ProWorkorderBom bom = new ProWorkorderBom(); bom.setLineId(lineId); bom.setWorkorderId(1L);
        bom.setItemId(itemId); bom.setItemCode("SEMI-" + itemId); bom.setItemName("半成品");
        bom.setQuantity(BigDecimal.TEN); bom.setUnitOfMeasure("pcs"); bom.setUnitName("件");
        return bom;
    }
}
