package com.ktg.mes;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.linkage.FormLinkagePolicy;
import com.ktg.mes.linkage.ProductReceiptLinkageService;
import com.ktg.mes.pro.controller.ProFeedbackController;
import com.ktg.mes.pro.domain.*;
import com.ktg.mes.pro.mapper.ProRouteMapper;
import com.ktg.mes.pro.mapper.ProRouteProductBomMapper;
import com.ktg.mes.pro.service.*;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.FeedbackBatchSourceMapper;
import com.ktg.mes.wm.service.IWmItemConsumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProFeedbackBatchExecutionTest {
    private final ProFeedbackController controller = new ProFeedbackController() {
        @Override public String getUsername() { return "tester"; }
    };
    private final IProFeedbackService feedbacks = mock(IProFeedbackService.class);
    private final IWmItemConsumeService consumes = mock(IWmItemConsumeService.class);
    private final ProFeedback feedback = new ProFeedback();
    private final Map<String, List<String>> scope = Collections.singletonMap("50", Collections.singletonList("THIS-RUN"));

    @BeforeEach
    void setUp() {
        PostingLockMapper locks = mock(PostingLockMapper.class);
        when(locks.feedback(1L)).thenReturn(UserConstants.FEEDBACK_STATUS_PREPARE);
        ReflectionTestUtils.setField(controller, "postingLockMapper", locks);
        ReflectionTestUtils.setField(controller, "proFeedbackService", feedbacks);
        ReflectionTestUtils.setField(controller, "wmItemConsumeService", consumes);
        ReflectionTestUtils.setField(controller, "formLinkagePolicy", new FormLinkagePolicy());
        ReflectionTestUtils.setField(controller, "productReceiptLinkageService", mock(ProductReceiptLinkageService.class));
        IProTaskService tasks = mock(IProTaskService.class);
        ReflectionTestUtils.setField(controller, "proTaskService", tasks);
        ProTask task = new ProTask(); task.setStatus(UserConstants.ORDER_STATUS_CONFIRMED);
        when(tasks.selectProTaskByTaskId(2L)).thenReturn(task);
        IProRouteProcessService rules = mock(IProRouteProcessService.class);
        ReflectionTestUtils.setField(controller, "proRouteProcessService", rules);
        ProRouteProcess rule = new ProRouteProcess(); rule.setIsCheck(UserConstants.NO);
        when(rules.selectProRouteProcessList(any())).thenReturn(Collections.singletonList(rule));
        feedback.setRecordId(1L); feedback.setTaskId(2L); feedback.setRouteId(40L);
        feedback.setProcessId(20L); feedback.setWorkorderId(30L); feedback.setItemId(10L);
        feedback.setStatus(UserConstants.FEEDBACK_STATUS_PREPARE);
        feedback.setQuantityFeedback(BigDecimal.TEN); feedback.setQuantityQualified(BigDecimal.TEN);
        feedback.setQuantityUnquanlified(BigDecimal.ZERO); feedback.setQuantityUncheck(BigDecimal.ZERO);
        when(feedbacks.selectProFeedbackByRecordId(1L)).thenReturn(feedback);
    }

    @Test
    void originalExecuteLongRemainsCompatibleWithoutScopeService() {
        AtomicReference<Boolean> constrained = new AtomicReference<>();
        when(consumes.generateItemConsume(any())).thenAnswer(invocation -> {
            constrained.set(feedback.getParams().containsKey("feedbackBatchScope")); return null;
        });
        assertEquals(200, controller.execute(1L).get("code"));
        assertEquals(Boolean.FALSE, constrained.get());
        assertEquals(UserConstants.FEEDBACK_STATUS_FINISHED, feedback.getStatus());
    }

    @Test
    void constrainedEndpointKeepsPermissionAndTransactionBoundary() {
        Method method = endpoint();
        assertEquals("/{recordId}/with-batches", method.getAnnotation(PutMapping.class).value()[0]);
        assertEquals("@ss.hasPermi('mes:pro:feedback:edit')", method.getAnnotation(PreAuthorize.class).value());
        assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    void nullBodyCannotFallBackToUnconstrainedExecution() {
        assertThrows(ServiceException.class, () -> execute(null));
        verify(consumes, never()).generateItemConsume(any());
    }

    @Test
    void validatedScopeIsAvailableOnlyDuringConsumptionGeneration() {
        installScopeService();
        AtomicReference<Object> actualScope = new AtomicReference<>();
        when(consumes.generateItemConsume(any())).thenAnswer(invocation -> {
            actualScope.set(feedback.getParams().get("feedbackBatchScope")); return null;
        });
        assertEquals(200, execute(scope).get("code"));
        assertEquals(scope, actualScope.get());
        assertFalse(feedback.getParams().containsKey("feedbackBatchScope"));
        assertEquals(UserConstants.FEEDBACK_STATUS_FINISHED, feedback.getStatus());
    }

    @Test
    void insufficientScopePropagatesForTransactionRollbackAndDoesNotCompleteFeedback() {
        installScopeService();
        when(consumes.generateItemConsume(any())).thenThrow(new ServiceException("指定批次库存不足"));
        assertThrows(ServiceException.class, () -> execute(scope));
        assertEquals(UserConstants.FEEDBACK_STATUS_PREPARE, feedback.getStatus());
        assertFalse(feedback.getParams().containsKey("feedbackBatchScope"));
        verify(feedbacks, never()).updateProFeedback(any());
    }

    @Test
    void transactionRollsBackWhenConstrainedInventoryIsInsufficient() {
        installScopeService();
        when(consumes.generateItemConsume(any())).thenThrow(new ServiceException("指定批次库存不足"));
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        TransactionStatus transaction = mock(TransactionStatus.class);
        when(transactions.getTransaction(any())).thenReturn(transaction);
        ProxyFactory factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        ProFeedbackController transactional = (ProFeedbackController) factory.getProxy();
        assertThrows(ServiceException.class, () -> transactional.executeWithBatches(1L, scope));
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
    }

    @Test
    void unrelatedBatchAndMissingItemFailBeforeConsumption() {
        installScopeService();
        assertThrows(ServiceException.class, () -> execute(Collections.singletonMap("50", Collections.singletonList("HISTORY"))));
        assertThrows(ServiceException.class, () -> execute(Collections.emptyMap()));
        verify(consumes, never()).generateItemConsume(any());
        assertEquals(UserConstants.FEEDBACK_STATUS_PREPARE, feedback.getStatus());
    }

    private void installScopeService() {
        ProRouteMapper routes = mock(ProRouteMapper.class);
        ProRouteProductBomMapper boms = mock(ProRouteProductBomMapper.class);
        FeedbackBatchSourceMapper sources = mock(FeedbackBatchSourceMapper.class);
        ProRoute route = new ProRoute(); route.setRouteId(40L);
        when(routes.getRouteByProductId(10L)).thenReturn(route);
        ProRouteProductBom bom = new ProRouteProductBom(); bom.setItemId(50L); bom.setItemCode("RM-50");
        when(boms.selectProRouteProductBomList(any())).thenReturn(Collections.singletonList(bom));
        when(sources.selectAllowedBatchCodes(30L, 50L)).thenReturn(Collections.singletonList("THIS-RUN"));
        ReflectionTestUtils.setField(controller, "feedbackBatchScopeService", new FeedbackBatchScopeService(routes, boms, sources));
    }

    private Method endpoint() {
        return assertDoesNotThrow(() -> ProFeedbackController.class.getMethod("executeWithBatches", Long.class, Map.class));
    }

    private AjaxResult execute(Map<String, List<String>> request) {
        return controller.executeWithBatches(1L, request);
    }
}
