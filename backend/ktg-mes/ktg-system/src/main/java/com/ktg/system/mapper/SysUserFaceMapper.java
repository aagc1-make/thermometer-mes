package com.ktg.system.mapper;

import com.ktg.system.domain.SysUserFace;

/**
 * 用户人脸登录模板 数据层
 */
public interface SysUserFaceMapper {

    /** 按用户查询人脸模板 */
    SysUserFace selectByUserId(Long userId);

    /** 新增模板 */
    int insertSysUserFace(SysUserFace face);

    /** 更新模板 */
    int updateSysUserFace(SysUserFace face);

    /** 删除模板 */
    int deleteByUserId(Long userId);
}
