package com.ktg.mes.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AiBusinessRulesTest {
    private final ObjectMapper json = new ObjectMapper();

    private String codeOf(String question) {
        AiBusinessRules.Call call = AiBusinessRules.detect(question, json);
        return call == null ? null : call.code;
    }

    /** “我们生产什么”问的是产品档案：答成工单进度或“没有查到这个物料”都是答非所问。 */
    @Test
    public void productQuestionsReadProductCatalog() {
        assertEquals("products", codeOf("我们工业生产的成品是什么"));
        assertEquals("products", codeOf("我们要生产什么"));
        assertEquals("products", codeOf("本厂生产的产品有哪些"));
        assertEquals("products", codeOf("咱们厂主要做什么产品"));
        assertEquals("products", codeOf("公司产品有哪些"));
        assertEquals("products", codeOf("工厂生产多少种产品"));
    }

    @Test
    public void productCatalogTakesNoDateOrMaterialFilter() {
        AiBusinessRules.Call call = AiBusinessRules.detect("10月份生产的成品是什么", json);

        assertEquals("products", call.code);
        assertFalse(call.args.has("from"));
        assertFalse(call.args.has("to"));
        assertFalse(call.args.has("query"));
    }

    @Test
    public void materialCodeStillFiltersTheCatalog() {
        AiBusinessRules.Call call = AiBusinessRules.detect("TG-FG-001 是成品吗", json);

        assertEquals("products", call.code);
        assertEquals("TG-FG-001", call.args.get("query").asText());
    }

    /** 带库存、BOM、单据、工序口径的问题仍归各自工具，不被产品目录抢走。 */
    @Test
    public void scopedQuestionsKeepTheirOwnTool() {
        assertEquals("stock", codeOf("TG-EL-002 现在库存多少"));
        assertEquals("bom", codeOf("TG-200 的 BOM 有哪些组件"));
        assertEquals("movements", codeOf("这个月入库多少"));
        assertEquals("inspections", codeOf("最近的 IQC 检验记录"));
        assertNull(codeOf("成品入库有多少"));
        assertNull(codeOf("哪些产品用上壳"));
    }

    @Test
    public void stockQueryExtractsTheMaterialNameWithoutTheSearchCommand() {
        AiBusinessRules.Call call = assertMaterialQuery("stock", "上壳", "查上壳库存");
        assertFalse(call.args.has("from"));
        assertFalse(call.args.has("to"));

        assertMaterialQuery("stock", "上壳", "请帮我查一下上壳的库存");
        assertMaterialQuery("stock", "上壳", "请问，查上壳库存");
        assertMaterialQuery("stock", "上壳", "帮我查下目前上壳库存");
        assertMaterialQuery("stock", "下壳", "查下壳库存");
        assertMaterialQuery("stock", "下壳", "帮我看下壳库存");
        assertMaterialQuery("stock", "上壳", "上壳是否有库存");
        assertMaterialQuery("stock", "TG-ST-001", "查 tg-st-001 库存");
        AiBusinessRules.Call historical = assertMaterialQuery("stock", "上壳", "2026-10-01 至 2026-10-15 查上壳库存");
        assertEquals("2026-10-01", historical.args.path("from").asText());
        assertEquals("2026-10-15", historical.args.path("to").asText());
    }

    @Test
    public void otherMaterialToolsExtractTheSameSearchTerms() {
        AiBusinessRules.Call movements = assertMaterialQuery("movements", "上壳", "2026年10月查上壳出入库");
        assertEquals("2026-10-01", movements.args.path("from").asText());
        assertEquals("2026-10-31", movements.args.path("to").asText());
        assertEquals("ALL", movements.args.path("direction").asText());

        AiBusinessRules.Call inspections = assertMaterialQuery("inspections", "上壳", "查查上壳的来料检验记录");
        assertEquals("IQC", inspections.args.path("kind").asText());
        assertMaterialQuery("bom", "红外测温枪整机", "请查红外测温枪整机的BOM");
        assertMaterialQuery("materials", "上壳", "帮我查看上壳的物料编码");
    }

    @Test
    public void alertsKeepTheNamedMaterialAndLeaveGeneralQuestionsUnfiltered() {
        AiBusinessRules.Call named = assertMaterialQuery("alerts", "上壳", "查上壳的安全库存预警");
        assertFalse(named.args.has("from"));
        assertMaterialQuery("alerts", "电池", "电池缺货吗");
        assertMaterialQuery("alerts", "TG-ST-001", "TG-ST-001 低于安全库存吗");

        AiBusinessRules.Call all = AiBusinessRules.detect("现在哪些物料低于安全库存？", json);
        assertEquals("alerts", all.code);
        assertFalse(all.args.has("query"));
        for (String question : new String[] {"查看全部物料的库存预警", "缺货的物料有哪些？", "还有哪些物料缺货",
            "有没有物料缺货", "有没有物料低于安全库存", "是否有物料低于安全库存", "还有库存预警吗"}) {
            assertFalse(question, AiBusinessRules.detect(question, json).args.has("query"));
        }
    }

    private AiBusinessRules.Call assertMaterialQuery(String tool, String item, String question) {
        AiBusinessRules.Call call = AiBusinessRules.detect(question, json);
        assertEquals(question, tool, call.code);
        assertEquals(question, item, call.args.path("query").asText());
        return call;
    }

    @Test
    public void workorderQuestionsAreUnchanged() {
        AiBusinessRules.Call overdue = AiBusinessRules.detect("哪些工单逾期", json);
        AiBusinessRules.Call one = AiBusinessRules.detect("MO202610310002 工单进度", json);

        assertEquals("workorders", overdue.code);
        assertTrue(overdue.args.get("overdue").asBoolean());
        assertEquals("workorders", one.code);
        assertEquals("MO202610310002", one.args.get("workorderCode").asText());
    }

    @Test
    public void writeActionsAreNotCatalogQuestions() {
        assertTrue(AiBusinessRules.preserveExistingAction("帮我新增一个到货通知"));
        assertFalse(AiBusinessRules.productCatalog("帮我新增一个到货通知"));
        assertNull(codeOf("帮我新增一个到货通知"));
    }
}
