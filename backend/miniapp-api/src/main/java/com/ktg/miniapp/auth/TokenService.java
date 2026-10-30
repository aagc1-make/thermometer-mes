package com.ktg.miniapp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量令牌服务：payload(base64url) + "." + HMAC-SHA256 签名(base64url)
 * 不引入额外依赖，令牌自带过期时间，服务端无需存储会话。
 */
@Component
public class TokenService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] secret;
    private final long expireSeconds;

    public TokenService(@Value("${miniapp.auth.secret:ktg-mes-miniapp-demo-secret}") String secret,
                        @Value("${miniapp.auth.expire-seconds:43200}") long expireSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireSeconds = expireSeconds;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    /** 签发令牌 */
    public String issue(long accountId, long clientId, String username) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("aid", accountId);
        payload.put("cid", clientId);
        payload.put("un", username);
        payload.put("exp", System.currentTimeMillis() + expireSeconds * 1000L);
        try {
            String body = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mapper.writeValueAsBytes(payload));
            return body + "." + sign(body);
        } catch (Exception e) {
            throw new IllegalStateException("签发令牌失败", e);
        }
    }

    /** 校验令牌；无效或过期返回 null，通过返回 payload */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return null;
        }
        String body = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                sign(body).getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            Map<String, Object> payload = mapper.readValue(Base64.getUrlDecoder().decode(body), Map.class);
            Object exp = payload.get("exp");
            if (!(exp instanceof Number) || ((Number) exp).longValue() < System.currentTimeMillis()) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签名失败", e);
        }
    }
}
