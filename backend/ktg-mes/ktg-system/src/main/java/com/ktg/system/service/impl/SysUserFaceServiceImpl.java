package com.ktg.system.service.impl;

import com.ktg.common.utils.DateUtils;
import com.ktg.system.domain.SysUserFace;
import com.ktg.system.face.FaceMatchResult;
import com.ktg.system.face.FaceTemplate;
import com.ktg.system.mapper.SysUserFaceMapper;
import com.ktg.system.service.ISysUserFaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 用户人脸登录模板 业务实现
 *
 * 只做特征存取与比对，不保存照片、不调用外部人脸服务。
 */
@Service
public class SysUserFaceServiceImpl implements ISysUserFaceService {

    /** 状态：正常 */
    private static final String STATUS_NORMAL = "0";

    @Autowired
    private SysUserFaceMapper userFaceMapper;

    /** 比对阈值：欧氏距离小于等于该值判定为同一人，可按设备与场景调整。 */
    @Value("${mes.face.threshold:0.55}")
    private double threshold;

    @Override
    public SysUserFace selectByUserId(Long userId) {
        return userFaceMapper.selectByUserId(userId);
    }

    @Override
    public int enroll(SysUserFace face) {
        if (face == null || face.getUserId() == null || face.getDescriptor() == null) {
            throw new IllegalArgumentException("人脸模板不完整");
        }
        double[] descriptor = FaceTemplate.parse(face.getDescriptor());
        if (!FaceTemplate.isUsable(descriptor)) {
            throw new IllegalArgumentException("人脸特征无效，请重新采集");
        }
        face.setDescriptor(FaceTemplate.format(descriptor));
        if (face.getStatus() == null) {
            face.setStatus(STATUS_NORMAL);
        }
        SysUserFace existing = userFaceMapper.selectByUserId(face.getUserId());
        if (existing == null) {
            face.setCreateTime(DateUtils.getNowDate());
            return userFaceMapper.insertSysUserFace(face);
        }
        face.setCreateTime(existing.getCreateTime());
        face.setUpdateTime(DateUtils.getNowDate());
        return userFaceMapper.updateSysUserFace(face);
    }

    @Override
    public int deleteByUserId(Long userId) {
        return userFaceMapper.deleteByUserId(userId);
    }

    @Override
    public FaceMatchResult verify(Long userId, double[] descriptor) {
        if (!FaceTemplate.isUsable(descriptor)) {
            return FaceMatchResult.invalidTemplate();
        }
        SysUserFace stored = userFaceMapper.selectByUserId(userId);
        if (stored == null || !STATUS_NORMAL.equals(stored.getStatus())) {
            return FaceMatchResult.notEnrolled();
        }
        double[] template = FaceTemplate.parse(stored.getDescriptor());
        if (!FaceTemplate.isUsable(template)) {
            return FaceMatchResult.invalidTemplate();
        }
        double distance = FaceTemplate.distance(template, descriptor);
        return distance <= threshold ? FaceMatchResult.matched(distance) : FaceMatchResult.notMatched(distance);
    }
}
