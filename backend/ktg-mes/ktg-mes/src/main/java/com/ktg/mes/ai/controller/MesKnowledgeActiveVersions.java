package com.ktg.mes.ai.controller;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

final class MesKnowledgeActiveVersions {

    private MesKnowledgeActiveVersions() {
    }

    static Map<Long, Map<String, Object>> load(JdbcTemplate jdbc, long ownerId) {
        Map<Long, Map<String, Object>> versions = new HashMap<>();

        jdbc.queryForList(
                "SELECT v.version_id, v.document_id, v.original_name, v.version_no "
                        + "FROM mes_kb_document d "
                        + "JOIN mes_kb_version v ON v.version_id = d.active_version_id "
                        + "AND v.document_id = d.document_id "
                        + "WHERE d.owner_id = ? AND d.deleted = 0 AND v.index_status = 'READY'",
                ownerId
        ).forEach(row -> versions.put(((Number) row.get("version_id")).longValue(), row));

        return versions;
    }
}