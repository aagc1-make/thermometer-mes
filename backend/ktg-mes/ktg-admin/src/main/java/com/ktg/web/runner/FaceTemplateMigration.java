package com.ktg.web.runner;

import java.util.List;
import com.ktg.system.domain.SysUserFace;
import com.ktg.system.face.FaceTemplate;
import com.ktg.system.face.FaceTemplateCipher;
import com.ktg.system.face.QdrantFaceTemplateStore;
import com.ktg.system.mapper.SysUserFaceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Migrates face templates in place when a template encryption key is configured. */
@Component
public class FaceTemplateMigration implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(FaceTemplateMigration.class);

    private final SysUserFaceMapper mapper;
    private final FaceTemplateCipher cipher;
    private final QdrantFaceTemplateStore vectorStore;
    private final TransactionTemplate transaction;
    private final boolean faceLoginEnabled;

    public FaceTemplateMigration(SysUserFaceMapper mapper, FaceTemplateCipher cipher,
                                 QdrantFaceTemplateStore vectorStore, PlatformTransactionManager transactionManager,
                                 @Value("${mes.face.enabled:true}") boolean faceLoginEnabled) {
        this.mapper = mapper;
        this.cipher = cipher;
        this.vectorStore = vectorStore;
        this.transaction = new TransactionTemplate(transactionManager);
        this.faceLoginEnabled = faceLoginEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!cipher.isConfigured()) {
            if (faceLoginEnabled) {
                LOG.warn("未配置有效的人脸模板加密密钥，暂停人脸录入和登录");
            }
            return;
        }
        if (vectorStore.isConfigured()) {
            try {
                migrateToQdrant();
            } catch (RuntimeException e) {
                LOG.error("专用人脸模板库尚未就绪，暂时无法使用人脸录入和登录（{}）", e.getClass().getSimpleName());
            }
            return;
        }

        long afterUserId = 0;
        int migrated = 0;
        int invalid = 0;
        List<SysUserFace> batch = mapper.selectLegacyTemplates(afterUserId);
        while (!batch.isEmpty()) {
            for (SysUserFace row : batch) {
                afterUserId = row.getUserId();
                String previous = row.getDescriptor();
                double[] descriptor = FaceTemplate.parse(previous);
                if (!FaceTemplate.isUsable(descriptor)) {
                    invalid++;
                    continue;
                }
                String encrypted = cipher.encrypt(afterUserId, FaceTemplate.format(descriptor));
                // A later enrollment or unbind takes precedence over the migration.
                migrated += mapper.replaceLegacyDescriptor(row.getUserId(), previous, encrypted);
            }
            batch = mapper.selectLegacyTemplates(afterUserId);
        }
        LOG.info("人脸模板加密已启用，迁移旧模板 {} 条", migrated);
        if (invalid > 0) {
            LOG.warn("有 {} 条旧人脸模板格式无效，需要重新录入", invalid);
        }
    }

    private void migrateToQdrant() {
        vectorStore.initialize();
        long afterUserId = 0;
        int transferred = 0;
        int invalid = 0;
        List<Long> batch = mapper.selectTemplateUserIds(afterUserId);
        while (!batch.isEmpty()) {
            for (Long userId : batch) {
                afterUserId = userId;
                int result = transaction.execute(status -> transferTemplate(userId));
                if (result < 0) {
                    invalid++;
                } else {
                    transferred += result;
                }
            }
            batch = mapper.selectTemplateUserIds(afterUserId);
        }
        vectorStore.markReady();
        LOG.info("专用人脸模板库已就绪，迁移旧模板 {} 条", transferred);
        if (invalid > 0) {
            LOG.warn("有 {} 条旧人脸模板无法迁移，需要重新录入", invalid);
        }
    }

    private int transferTemplate(Long userId) {
        if (mapper.lockActiveAccount(userId) == null) {
            return 0;
        }
        SysUserFace legacy = mapper.selectTemplateForMigration(userId);
        if (legacy == null) {
            return 0;
        }
        String stored = legacy.getDescriptor();
        boolean encrypted = stored.startsWith("gcm:");
        double[] descriptor = FaceTemplate.parse(encrypted ? cipher.decrypt(userId, stored) : stored);
        if (!FaceTemplate.isUsable(descriptor)) {
            return -1;
        }

        SysUserFace existing = vectorStore.find(userId, true);
        if (existing == null) {
            if (!encrypted) {
                legacy.setDescriptor(cipher.encrypt(userId, FaceTemplate.format(descriptor)));
            }
            vectorStore.put(legacy);
        } else if (!FaceTemplate.isUsable(FaceTemplate.parse(cipher.decrypt(userId, existing.getDescriptor())))) {
            // Preserve the source when a previous migration has not left a readable copy.
            return -1;
        }
        return mapper.deleteByUserId(userId);
    }
}
