package com.ktg.system.face;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Encrypts stored face templates and authenticates their account association. */
@Component
public class FaceTemplateCipher {
    private static final Logger LOG = LoggerFactory.getLogger(FaceTemplateCipher.class);
    private static final String FORMAT = "gcm:v1:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final byte[] PURPOSE = "mes.face.template:gcm:v1".getBytes(StandardCharsets.UTF_8);
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public FaceTemplateCipher(@Value("${mes.face.encryption-key:}") String configuredKey) {
        String environmentKey = System.getenv("MES_FACE_TEMPLATE_KEY");
        key = loadKey(environmentKey == null ? configuredKey : environmentKey);
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(long userId, String descriptor) {
        if (!isConfigured()) {
            throw new IllegalStateException("人脸模板加密密钥不可用");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        try {
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, userId, nonce);
            byte[] ciphertext = cipher.doFinal(descriptor.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = ByteBuffer.allocate(nonce.length + ciphertext.length).put(nonce).put(ciphertext).array();
            return FORMAT + Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException ignored) {
            throw new IllegalStateException("人脸模板加密失败");
        }
    }

    public String decrypt(long userId, String stored) {
        if (!isConfigured() || stored == null || !stored.startsWith(FORMAT)) {
            return null;
        }
        try {
            byte[] envelope = Base64.getDecoder().decode(stored.substring(FORMAT.length()));
            if (envelope.length <= NONCE_BYTES + TAG_BITS / Byte.SIZE) {
                return null;
            }
            byte[] nonce = Arrays.copyOf(envelope, NONCE_BYTES);
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE, userId, nonce);
            return new String(cipher.doFinal(envelope, NONCE_BYTES, envelope.length - NONCE_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private Cipher initCipher(int mode, long userId, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(PURPOSE);
        cipher.updateAAD(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
        return cipher;
    }

    private static SecretKeySpec loadKey(String configuredKey) {
        if (configuredKey == null || configuredKey.trim().isEmpty()) {
            return null;
        }
        byte[] decoded = null;
        try {
            decoded = Base64.getDecoder().decode(configuredKey.trim());
            if (decoded.length == KEY_BYTES && Cipher.getMaxAllowedKeyLength(ALGORITHM) >= KEY_BYTES * Byte.SIZE) {
                Cipher.getInstance(ALGORITHM);
                return new SecretKeySpec(decoded, "AES");
            }
        } catch (GeneralSecurityException | IllegalArgumentException ignored) {
            // Report configuration errors without exposing the supplied key.
        } finally {
            if (decoded != null) {
                Arrays.fill(decoded, (byte) 0);
            }
        }
        LOG.warn("人脸模板加密密钥无效或运行环境不支持 AES-256-GCM");
        return null;
    }
}
