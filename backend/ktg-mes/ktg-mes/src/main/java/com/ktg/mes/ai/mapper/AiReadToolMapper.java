package com.ktg.mes.ai.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AiReadToolMapper {
    List<Map<String, Object>> stock(Map<String, Object> args);
    List<Map<String, Object>> movements(Map<String, Object> args);
    int countPurchaseReceipts(Map<String, Object> args);
    List<Map<String, Object>> purchaseReceiptRows(Map<String, Object> args);
    List<Map<String, Object>> workorders(Map<String, Object> args);
    List<Map<String, Object>> products(Map<String, Object> args);
    List<Map<String, Object>> materialCandidates(Map<String, Object> args);
    List<Map<String, Object>> similarItems(Map<String, Object> args);
    List<Map<String, Object>> bom(Map<String, Object> args);
    List<Map<String, Object>> inspections(Map<String, Object> args);
    List<Map<String, Object>> alerts(Map<String, Object> args);
    List<Map<String, Object>> materials(Map<String, Object> args);
    List<Map<String, Object>> searchItems(Map<String, Object> args);

    /** 影响路径：按完整编码取物料主数据（起点）。 */
    Map<String, Object> impactItem(@Param("itemCode") String itemCode);

    /** 影响路径：一批子件 ID 反查启用的父件装配行。 */
    List<Map<String, Object>> impactParents(@Param("childIds") List<Long> childIds, @Param("limit") int limit);

    /** 影响路径：按有序行号读实时 BOM 行，用于逐段校验链条并返回明细。 */
    List<Map<String, Object>> impactPathRows(@Param("bomIds") List<Long> bomIds);

    /** 影响路径：问答里提到完整编码时按编码取物料。 */
    List<Map<String, Object>> impactItemsByCodes(@Param("codes") List<String> codes);

    /** 影响路径：问答没有完整编码时，按问题中包含的物料名称取候选。 */
    List<Map<String, Object>> impactItemsByName(@Param("question") String question, @Param("limit") int limit);
}
