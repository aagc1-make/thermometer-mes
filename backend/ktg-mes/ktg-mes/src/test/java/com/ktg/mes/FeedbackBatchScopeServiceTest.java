package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.pro.domain.ProFeedback;
import com.ktg.mes.pro.domain.ProRoute;
import com.ktg.mes.pro.domain.ProRouteProductBom;
import com.ktg.mes.pro.mapper.ProRouteMapper;
import com.ktg.mes.pro.mapper.ProRouteProductBomMapper;
import com.ktg.mes.pro.service.FeedbackBatchScopeService;
import com.ktg.mes.wm.mapper.FeedbackBatchSourceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeedbackBatchScopeServiceTest {
    private final ProRouteMapper routes = mock(ProRouteMapper.class);
    private final ProRouteProductBomMapper boms = mock(ProRouteProductBomMapper.class);
    private final ProFeedback feedback = new ProFeedback();
    private final FeedbackBatchSourceMapper sources = mock(FeedbackBatchSourceMapper.class);
    private final FeedbackBatchScopeService service = new FeedbackBatchScopeService(routes, boms, sources);
    private List<String> sourceBatches = Collections.singletonList("THIS-RUN");

    @BeforeEach
    void setUp() {
        feedback.setItemId(10L); feedback.setProcessId(20L); feedback.setWorkorderId(30L);
        ProRoute route = new ProRoute(); route.setRouteId(40L);
        when(routes.getRouteByProductId(10L)).thenReturn(route);
        ProRouteProductBom bom = new ProRouteProductBom(); bom.setItemId(50L); bom.setItemCode("RM-50");
        when(boms.selectProRouteProductBomList(any())).thenReturn(Collections.singletonList(bom));
    }

    @Test
    void rejectsBatchNotIssuedToThisWorkorderOrReleasedByItsDirectChild() {
        ServiceException failure = assertThrows(ServiceException.class,
                () -> validate(Collections.singletonMap("50", Collections.singletonList("OTHER-WORKORDER"))));
        assertTrue(failure.getMessage().contains("OTHER-WORKORDER"));
    }

    @Test
    void everyActualConsumptionItemMustHaveScope() {
        assertThrows(ServiceException.class, () -> validate(Collections.emptyMap()));
    }

    @Test
    void emptyOrBlankBatchDoesNotEnableUnbatchedConsumption() {
        assertThrows(ServiceException.class, () -> validate(Collections.singletonMap("50", Collections.emptyList())));
        assertThrows(ServiceException.class, () -> validate(Collections.singletonMap("50", Collections.singletonList(" "))));
    }

    @Test
    void oneValidBatchDoesNotPermitAnUnrelatedBatchInTheSameScope() {
        assertThrows(ServiceException.class,
                () -> validate(Collections.singletonMap("50", Arrays.asList("THIS-RUN", "HISTORY"))));
    }

    @Test
    void scopeReturnsOnlyValidatedActualBomItems() {
        Map<String, List<String>> request = new LinkedHashMap<>();
        request.put("50", Arrays.asList("THIS-RUN", "THIS-RUN"));
        request.put("999", Collections.singletonList("UNUSED"));
        assertEquals(Collections.singletonMap("50", Collections.singletonList("THIS-RUN")), validate(request));
    }

    @Test
    void noServerSideSourceDoesNotTrustRequestedBatch() {
        sourceBatches = Collections.emptyList();
        assertThrows(ServiceException.class,
                () -> validate(Collections.singletonMap("50", Collections.singletonList("THIS-RUN"))));
    }

    @Test
    void processWithoutConsumptionAcceptsEmptyScope() {
        when(boms.selectProRouteProductBomList(any())).thenReturn(Collections.emptyList());
        assertEquals(Collections.emptyMap(), validate(Collections.emptyMap()));
    }

    private Map<String, List<String>> validate(Map<String, List<String>> request) {
        when(sources.selectAllowedBatchCodes(30L, 50L)).thenReturn(sourceBatches);
        return service.validate(feedback, request);
    }
}
