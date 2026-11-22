package com.ktg.system.service;

import com.ktg.system.domain.SysUserFace;
import com.ktg.system.domain.SysUserFaceAdminRow;
import com.ktg.system.domain.SysUserFaceStatus;
import com.ktg.system.face.FaceMatchResult;

import java.util.List;

/**
 * 用户人脸登录模板 业务层
 */
public interface ISysUserFaceService {

    /** 查询用户的模板录入元数据（可能为 null）。 */
    SysUserFace selectByUserId(Long userId);

    /** 人脸录入和登录所需的配置与模板存储是否就绪。 */
    boolean isFaceServiceAvailable();

    /** 账号状态和管理策略是否允许人脸登录。 */
    boolean isAccountFaceLoginAllowed(Long userId);

    /** 查询用户管理所需的录入状态和登录策略。 */
    SysUserFaceStatus getFaceStatus(Long userId);

    /** 更新人脸登录策略并返回最新状态。 */
    SysUserFaceStatus updateFaceLoginPolicy(Long userId, boolean enabled);

    /** 录入或更新人脸模板 */
    int enroll(SysUserFace face);

    /** 追加一个补充角度模板（最多 3 个） */
    int appendTemplate(SysUserFace face);

    /** 已保存的模板数量：主模板 + 补充角度 */
    int countTemplates(Long userId);

    /** 模板算法版本与当前算法不一致，需要重新录入 */
    boolean needsReenroll(Long userId);

    /** 解绑：删除该用户的人脸模板 */
    int deleteByUserId(Long userId);

    /** 1:1 比对当前采集到的特征与库内模板 */
    FaceMatchResult verify(Long userId, double[] descriptor);

    /** 管理员：人脸录入信息列表（只含元数据） */
    List<SysUserFaceAdminRow> selectFaceAdminList(String keyword, String filter);
}
