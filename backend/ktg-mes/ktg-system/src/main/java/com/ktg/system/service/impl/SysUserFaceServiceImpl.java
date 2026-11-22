package com.ktg.system.service.impl;

import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import com.ktg.system.domain.SysUserFace;
import com.ktg.system.domain.SysUserFaceAdminRow;
import com.ktg.system.domain.SysUserFaceStatus;
import com.ktg.system.face.FaceMatchResult;
import com.ktg.system.face.FaceTemplate;
import com.ktg.system.face.FaceTemplateCipher;
import com.ktg.system.face.QdrantFaceTemplateStore;
import com.ktg.system.mapper.SysUserFaceMapper;
import com.ktg.system.service.ISysUserFaceService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户人脸登录模板 业务实现
 *
 * 管理加密特征模板，按当前账号进行 1:1 比对。
 * 主模板存于 sys_user_face（或 Qdrant 专用集合），补充角度存于 sys_user_face_alt。
 */
@Service
public class SysUserFaceServiceImpl implements ISysUserFaceService {

    private static final Logger log = LoggerFactory.getLogger(SysUserFaceServiceImpl.class);

    /** 状态：正常 */
    private static final String STATUS_NORMAL = "0";

    /** 授权说明版本，随说明文字变更递增 */
    private static final String CONSENT_VERSION = "v1";

    /** 补充角度模板上限 */
    private static final int MAX_ALTERNATES = 3;

    @Autowired
    private SysUserFaceMapper userFaceMapper;

    @Autowired
    private FaceTemplateCipher templateCipher;

    @Autowired
    private QdrantFaceTemplateStore vectorStore;

    @Value("${mes.face.enabled:true}")
    private boolean faceEnabled;

    /** 比对阈值：欧氏距离小于等于该值判定为同一人，可按设备与场景调整。 */
    @Value("${mes.face.threshold:0.55}")
    private double threshold;

    @Override
    public SysUserFace selectByUserId(Long userId) {
        if (vectorStore.isConfigured()) {
            requireVectorStoreReady();
            return vectorStore.find(userId, false);
        }
        return userFaceMapper.selectByUserId(userId);
    }

    @Override
    public boolean isFaceServiceAvailable() {
        return faceEnabled && templateCipher.isConfigured() && (!vectorStore.isConfigured() || vectorStore.isReady());
    }

    @Override
    public boolean isAccountFaceLoginAllowed(Long userId) {
        return userFaceMapper.isFaceLoginAllowed(userId);
    }

    @Override
    public SysUserFaceStatus getFaceStatus(Long userId) {
        SysUserFaceStatus status = userFaceMapper.selectFaceStatus(userId);
        if (status != null && vectorStore.isConfigured()) {
            SysUserFace template = selectByUserId(userId);
            status.setEnrolled(template != null);
            status.setUpdatedAt(template == null ? null
                    : (template.getUpdateTime() == null ? template.getCreateTime() : template.getUpdateTime()));
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserFaceStatus updateFaceLoginPolicy(Long userId, boolean enabled) {
        userFaceMapper.saveFaceLoginPolicy(userId, enabled);
        return getFaceStatus(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int enroll(SysUserFace face) {
        if (!isFaceServiceAvailable()) {
            throw new ServiceException("人脸录入暂不可用，请联系管理员");
        }
        if (face == null || face.getUserId() == null || face.getDescriptor() == null) {
            throw new IllegalArgumentException("人脸模板不完整");
        }
        double[] descriptor = FaceTemplate.parse(face.getDescriptor());
        if (!FaceTemplate.isUsable(descriptor)) {
            throw new IllegalArgumentException("人脸特征无效，请重新采集");
        }
        face.setDescriptor(templateCipher.encrypt(face.getUserId(), FaceTemplate.format(descriptor)));
        face.setAlgorithm(FaceTemplate.ALGORITHM);
        if (face.getStatus() == null) {
            face.setStatus(STATUS_NORMAL);
        }
        if (face.getConsentAt() == null) {
            face.setConsentAt(DateUtils.getNowDate());
        }
        if (face.getConsentVersion() == null) {
            face.setConsentVersion(CONSENT_VERSION);
        }
        lockAccount(face.getUserId());
        SysUserFace existing = selectByUserId(face.getUserId());
        face.setCreateTime(existing == null ? DateUtils.getNowDate() : existing.getCreateTime());
        if (existing != null) {
            face.setUpdateTime(DateUtils.getNowDate());
        }
        // 专用人脸库是主存储：先写外部库，再清理 MySQL 旧行；清理失败只告警，不回滚已生效的模板
        if (vectorStore.isConfigured()) {
            vectorStore.put(face);
            try {
                userFaceMapper.deleteByUserId(face.getUserId());
            } catch (RuntimeException e) {
                log.warn("人脸模板已写入专用人脸库，MySQL 旧记录清理失败 user={}", face.getUserId(), e);
            }
            return 1;
        }
        if (existing == null) {
            return userFaceMapper.insertSysUserFace(face);
        }
        return userFaceMapper.updateSysUserFace(face);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int appendTemplate(SysUserFace face) {
        if (!isFaceTemplateStorageReady()) {
            throw new ServiceException("人脸录入暂不可用，请联系管理员");
        }
        if (face == null || face.getUserId() == null || face.getDescriptor() == null) {
            throw new IllegalArgumentException("人脸模板不完整");
        }
        double[] descriptor = FaceTemplate.parse(face.getDescriptor());
        if (!FaceTemplate.isUsable(descriptor)) {
            throw new IllegalArgumentException("人脸特征无效，请重新采集");
        }
        lockAccount(face.getUserId());
        if (!hasPrimaryTemplate(face.getUserId())) {
            throw new ServiceException("请先录入主模板，再追加角度");
        }
        int existing = userFaceMapper.countAlternates(face.getUserId());
        if (existing >= MAX_ALTERNATES) {
            throw new ServiceException("最多保存 " + MAX_ALTERNATES + " 个补充角度，请先更新主模板");
        }
        face.setSeq(existing + 1);
        face.setDescriptor(templateCipher.encrypt(face.getUserId(), FaceTemplate.format(descriptor)));
        face.setAlgorithm(FaceTemplate.ALGORITHM);
        face.setCreateTime(DateUtils.getNowDate());
        return userFaceMapper.insertAlternate(face);
    }

    @Override
    public int countTemplates(Long userId) {
        int count = hasPrimaryTemplate(userId) ? 1 : 0;
        return count + userFaceMapper.countAlternates(userId);
    }

    @Override
    public boolean needsReenroll(Long userId) {
        String algorithm;
        if (vectorStore.isConfigured()) {
            if (!vectorStore.isReady()) {
                return false;
            }
            try {
                SysUserFace template = vectorStore.find(userId, false);
                algorithm = template == null ? null : template.getAlgorithm();
            } catch (RuntimeException e) {
                return false;
            }
        } else {
            SysUserFace template = userFaceMapper.selectTemplateForMigration(userId);
            algorithm = template == null ? null : template.getAlgorithm();
        }
        return algorithm != null && !matchesAlgorithm(algorithm);
    }

    /** 主模板是否已存在：专用人脸库优先，未启用时读 MySQL。 */
    private boolean hasPrimaryTemplate(Long userId) {
        if (vectorStore.isConfigured()) {
            if (!vectorStore.isReady()) {
                return false;
            }
            try {
                return vectorStore.find(userId, false) != null;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return userFaceMapper.selectTemplateForMigration(userId) != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByUserId(Long userId) {
        lockAccount(userId);
        if (vectorStore.isConfigured()) {
            requireVectorStoreReady();
            vectorStore.delete(userId);
        }
        userFaceMapper.deleteAlternates(userId);
        return userFaceMapper.deleteByUserId(userId);
    }

    @Override
    public FaceMatchResult verify(Long userId, double[] descriptor) {
        if (!isFaceServiceAvailable() || !FaceTemplate.isUsable(descriptor)) {
            return FaceMatchResult.invalidTemplate();
        }
        List<String> stored = new ArrayList<String>();
        boolean stale = false;

        if (vectorStore.isConfigured()) {
            SysUserFace template = vectorStore.find(userId, true);
            if (template == null || !STATUS_NORMAL.equals(template.getStatus()) || !isAccountFaceLoginAllowed(userId)) {
                return FaceMatchResult.notEnrolled();
            }
            if (matchesAlgorithm(template.getAlgorithm())) {
                stored.add(template.getDescriptor());
            } else {
                stale = true;
            }
        } else {
            SysUserFace primary = userFaceMapper.selectTemplateForMigration(userId);
            if (primary == null || !STATUS_NORMAL.equals(primary.getStatus()) || !isAccountFaceLoginAllowed(userId)) {
                return FaceMatchResult.notEnrolled();
            }
            if (matchesAlgorithm(primary.getAlgorithm())) {
                stored.add(primary.getDescriptor());
            } else {
                stale = true;
            }
        }

        for (SysUserFace alternate : userFaceMapper.selectAlternates(userId)) {
            if (!matchesAlgorithm(alternate.getAlgorithm())) {
                stale = true;
                continue;
            }
            stored.add(alternate.getDescriptor());
        }

        double best = Double.NaN;
        for (String cipher : stored) {
            double[] template = FaceTemplate.parse(templateCipher.decrypt(userId, cipher));
            if (!FaceTemplate.isUsable(template)) {
                continue;
            }
            double distance = FaceTemplate.distance(template, descriptor);
            if (Double.isNaN(best) || distance < best) {
                best = distance;
            }
        }
        if (Double.isNaN(best)) {
            return stale ? FaceMatchResult.staleTemplate() : FaceMatchResult.invalidTemplate();
        }
        return best <= threshold ? FaceMatchResult.matched(best) : FaceMatchResult.notMatched(best);
    }

    @Override
    public List<SysUserFaceAdminRow> selectFaceAdminList(String keyword, String filter) {
        List<SysUserFaceAdminRow> rows = userFaceMapper.selectFaceAdminList(trim(keyword), trim(filter));
        for (SysUserFaceAdminRow row : rows) {
            row.setNeedReenroll(row.isEnrolled() && !matchesAlgorithm(row.getAlgorithm()));
        }
        return rows;
    }

    /** 当前特征算法是否与模板一致；模板未记录算法时按旧版本处理。 */
    private boolean matchesAlgorithm(String algorithm) {
        return FaceTemplate.ALGORITHM.equals(algorithm);
    }

    /** 模板存储是否就绪：加密密钥必须可用；启用专用人脸库时还要求它就绪。 */
    private boolean isFaceTemplateStorageReady() {
        return templateCipher.isConfigured() && (!vectorStore.isConfigured() || vectorStore.isReady());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void lockAccount(Long userId) {
        if (userFaceMapper.lockActiveAccount(userId) == null) {
            throw new ServiceException("用户不存在");
        }
    }

    private void requireVectorStoreReady() {
        if (!vectorStore.isReady()) {
            throw new ServiceException("人脸模板库暂不可用，请使用密码登录");
        }
    }
}
