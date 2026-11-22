package com.ktg.mes.ai.mapper;

import java.util.List;
import java.util.Map;

public interface AiReadToolMapper {
    List<Map<String, Object>> stock(Map<String, Object> args);
    List<Map<String, Object>> movements(Map<String, Object> args);
    int countPurchaseReceipts(Map<String, Object> args);
    List<Map<String, Object>> purchaseReceiptRows(Map<String, Object> args);
    List<Map<String, Object>> workorders(Map<String, Object> args);
    List<Map<String, Object>> materialCandidates(Map<String, Object> args);
    List<Map<String, Object>> bom(Map<String, Object> args);
    List<Map<String, Object>> inspections(Map<String, Object> args);
    List<Map<String, Object>> alerts(Map<String, Object> args);
    List<Map<String, Object>> materials(Map<String, Object> args);
}
