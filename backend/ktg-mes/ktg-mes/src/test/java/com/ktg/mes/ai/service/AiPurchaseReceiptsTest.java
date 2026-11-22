package com.ktg.mes.ai.service;

import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.mapper.AiReadToolMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AiPurchaseReceiptsTest {
    private Map<String, Object> countScope;
    private Map<String, Object> detailsScope;
    private final AiReadTools tools = new AiReadTools(new PermissionService() {
        @Override
        public boolean hasPermi(String permission) {
            return false;
        }
    }, testMapper());

    @SuppressWarnings("unchecked")
    private AiReadToolMapper testMapper() {
        return (AiReadToolMapper) Proxy.newProxyInstance(AiReadToolMapper.class.getClassLoader(),
            new Class<?>[]{AiReadToolMapper.class}, (proxy, method, args) -> {
                if ("countPurchaseReceipts".equals(method.getName())) {
                    countScope = (Map<String, Object>) args[0];
                    return 56;
                }
                if ("purchaseReceiptRows".equals(method.getName())) {
                    detailsScope = (Map<String, Object>) args[0];
                    return Collections.singletonList(Collections.<String, Object>singletonMap("receiptCode", "IR-001"));
                }
                throw new AssertionError(method.getName());
            });
    }

    @Test
    public void totalOnlyDoesNotLoadRows() {
        Map<String, Object> result = tools.purchaseReceipts("TG-01", "2026-11-01", "2026-11-30", null);

        assertEquals(56, result.get("total"));
        assertEquals(Collections.emptyList(), result.get("results"));
        assertEquals("TG-01", countScope.get("itemCode"));
        assertEquals("2026-11-01", countScope.get("from"));
        assertEquals("2026-11-30", countScope.get("to"));
        assertNull(detailsScope);
    }

    @Test
    public void detailQueryUsesSameScopeAndHidesVendorWithoutReceiptPermission() {
        Map<String, Object> answer = tools.purchaseReceipts("TG-01", "2026-11-01", "2026-11-30", 2);

        assertEquals(25, answer.get("offset"));
        assertEquals(3, answer.get("lastPage"));
        AiReadTools.Result result = (AiReadTools.Result) ((List<?>) answer.get("results")).get(0);
        assertTrue(result.getColumns().stream().anyMatch(column -> "receiptCode".equals(column.get("prop"))));
        assertFalse(result.getColumns().stream().anyMatch(column -> "vendorName".equals(column.get("prop"))));
        countScope.forEach((key, value) -> assertEquals(value, detailsScope.get(key)));
        assertEquals(25, detailsScope.get("offset"));
        assertEquals(false, detailsScope.get("includeVendor"));
    }

    @Test
    public void mappedCountAndDetailsUseIdenticalFilters() throws Exception {
        String resource = "mapper/ai/AiReadToolMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("itemCode", "TG-01");
        params.put("includeVendor", false);
        String prefix = AiReadToolMapper.class.getName() + ".";
        String count = configuration.getMappedStatement(prefix + "countPurchaseReceipts")
            .getBoundSql(params).getSql().replaceAll("\\s+", " ").trim();
        String details = configuration.getMappedStatement(prefix + "purchaseReceiptRows")
            .getBoundSql(params).getSql().replaceAll("\\s+", " ").trim();

        assertTrue(count.contains("count(*)"));
        assertFalse(count.contains("limit"));
        assertTrue(details.contains(count.substring(count.indexOf("where"))));
        assertFalse(details.contains("vendor_name"));
        assertTrue(details.contains("order by t.transaction_date desc, t.transaction_id desc limit ? offset ?"));
    }
}
