package com.ktg.miniapp.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 小程序账号数据访问 */
@Repository
public class AuthRepository {

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> findByUsername(String username) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT account_id AS accountId, client_id AS clientId, username, "
                        + "password_hash AS passwordHash, salt, display_name AS displayName, enabled "
                        + "FROM miniapp_account WHERE username = ?", username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findById(long accountId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT account_id AS accountId, client_id AS clientId, username, "
                        + "display_name AS displayName, enabled FROM miniapp_account WHERE account_id = ?", accountId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void touchLogin(long accountId) {
        jdbc.update("UPDATE miniapp_account SET last_login_time = NOW() WHERE account_id = ?", accountId);
    }
}
