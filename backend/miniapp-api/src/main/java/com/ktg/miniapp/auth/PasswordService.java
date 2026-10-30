package com.ktg.miniapp.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 密码摘要：SHA-256(salt + 明文)，十六进制小写 */
@Component
public class PasswordService {

    public String hash(String salt, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("密码摘要失败", e);
        }
    }

    public boolean matches(String salt, String rawPassword, String expectedHash) {
        if (expectedHash == null || salt == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(salt, rawPassword).getBytes(StandardCharsets.UTF_8),
                expectedHash.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
