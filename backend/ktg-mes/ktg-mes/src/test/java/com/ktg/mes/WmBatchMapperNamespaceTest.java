package com.ktg.mes;

import com.ktg.mes.wm.domain.WmBatch;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/** 验证真实 mapper 生成的查询边界，避免普通批次查询误复用试产命名空间。 */
class WmBatchMapperNamespaceTest {
    private Configuration configuration;

    @BeforeEach
    void loadActualMapper() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("WmBatch", WmBatch.class);
        String resource = "mapper/wm/WmBatchMapper.xml";
        try (InputStream xml = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void ordinaryNullLotRequiresSqlNullAndCannotSelectNonNullDemoLot() {
        WmBatch ordinary = item();
        BoundSql query = query(ordinary);
        assertTrue(normalize(query).contains("and lot_number is null"));
        assertFalse(query.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("lotNumber")));
    }

    @Test
    void emptyLotKeepsTheSameOrdinaryBatchBoundary() {
        WmBatch ordinary = item(); ordinary.setLotNumber("");
        assertTrue(normalize(query(ordinary)).contains("and lot_number is null"));
    }

    @Test
    void demoRunUsesExactParameterizedLotMatch() {
        WmBatch demo = item(); demo.setLotNumber("MES-DEMO:DRUN1");
        BoundSql query = query(demo);
        assertTrue(normalize(query).contains("and lot_number = ?"));
        assertFalse(normalize(query).contains("lot_number is null"));
        assertTrue(query.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("lotNumber")));
        assertEquals("MES-DEMO:DRUN1", configuration.newMetaObject(query.getParameterObject()).getValue("lotNumber"));
    }

    private BoundSql query(WmBatch batch) {
        return configuration.getMappedStatement("com.ktg.mes.wm.mapper.WmBatchMapper.getBatchCodeByParams")
                .getBoundSql(batch);
    }

    private String normalize(BoundSql query) { return query.getSql().replaceAll("\\s+", " ").toLowerCase(); }
    private WmBatch item() { WmBatch batch = new WmBatch(); batch.setItemId(10L); return batch; }
}
