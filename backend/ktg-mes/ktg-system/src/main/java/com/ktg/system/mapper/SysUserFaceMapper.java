package com.ktg.system.mapper;

import java.util.List;
import com.ktg.system.domain.SysUserFace;
import com.ktg.system.domain.SysUserFaceAdminRow;
import com.ktg.system.domain.SysUserFaceStatus;
import org.apache.ibatis.annotations.Param;

/**
 * 用户人脸登录模板 数据层
 */
public interface SysUserFaceMapper {

    /** 按用户查询录入元数据。 */
    SysUserFace selectByUserId(Long userId);

    List<SysUserFace> selectLegacyTemplates(@Param("afterUserId") long afterUserId);

    List<Long> selectTemplateUserIds(@Param("afterUserId") long afterUserId);

    SysUserFace selectTemplateForMigration(Long userId);

    Long lockActiveAccount(Long userId);

    boolean isFaceLoginAllowed(Long userId);

    int replaceLegacyDescriptor(@Param("userId") Long userId, @Param("previous") String previous,
                                @Param("encrypted") String encrypted);

    /** 查询用户管理所需的录入状态和登录策略。 */
    SysUserFaceStatus selectFaceStatus(Long userId);

    /** 更新账号的人脸登录策略。 */
    int saveFaceLoginPolicy(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    /** 新增模板 */
    int insertSysUserFace(SysUserFace face);

    /** 更新模板 */
    int updateSysUserFace(SysUserFace face);

    /** 删除模板 */
    int deleteByUserId(Long userId);

    /** 查询补充角度模板（加密特征） */
    List<SysUserFace> selectAlternates(Long userId);

    /** 补充角度模板数量 */
    int countAlternates(Long userId);

    /** 新增补充角度模板 */
    int insertAlternate(SysUserFace face);

    /** 删除全部补充角度模板 */
    int deleteAlternates(Long userId);

    /** 人脸管理列表：只返回元数据 */
    List<SysUserFaceAdminRow> selectFaceAdminList(@Param("keyword") String keyword, @Param("filter") String filter);
}
