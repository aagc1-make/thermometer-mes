package com.ktg.system.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 人脸管理列表行（管理员视图）。
 *
 * 只包含元数据，不含任何特征明文或密文。
 */
public class SysUserFaceAdminRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 登录名 */
    private String userName;

    /** 姓名 */
    private String nickName;

    /** 部门 */
    private String deptName;

    /** 是否已录入 */
    private boolean enrolled;

    /** 是否允许人脸登录 */
    private boolean enabled;

    /** 模板状态：0 正常，1 停用 */
    private String status;

    /** 模板算法版本 */
    private String algorithm;

    /** 录入样本帧数 */
    private Integer samples;

    /** 录入质量分 */
    private BigDecimal quality;

    /** 最近录入/更新时间 */
    private Date updatedAt;

    /** 补充角度模板数量 */
    private Integer altCount;

    /** 模板算法与当前版本不一致，需要重新录入 */
    private boolean needReenroll;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public void setEnrolled(boolean enrolled) {
        this.enrolled = enrolled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getSamples() {
        return samples;
    }

    public void setSamples(Integer samples) {
        this.samples = samples;
    }

    public BigDecimal getQuality() {
        return quality;
    }

    public void setQuality(BigDecimal quality) {
        this.quality = quality;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getAltCount() {
        return altCount;
    }

    public void setAltCount(Integer altCount) {
        this.altCount = altCount;
    }

    public boolean isNeedReenroll() {
        return needReenroll;
    }

    public void setNeedReenroll(boolean needReenroll) {
        this.needReenroll = needReenroll;
    }
}
