package com.ktg.system.service;

import com.ktg.system.domain.SysUserFace;
import com.ktg.system.face.FaceMatchResult;

/**
 * 用户人脸登录模板 业务层
 */
public interface ISysUserFaceService {

    /** 查询用户已录入的人脸模板（可能为 null） */
    SysUserFace selectByUserId(Long userId);

    /** 录入或更新人脸模板 */
    int enroll(SysUserFace face);

    /** 解绑：删除该用户的人脸模板 */
    int deleteByUserId(Long userId);

    /** 1:1 比对当前采集到的特征与库内模板 */
    FaceMatchResult verify(Long userId, double[] descriptor);
}
