package com.ktg.common.core.domain.model;

import java.util.List;

/**
 * 录入/解绑人脸模板的请求体。
 *
 * 两个操作都要求再次输入登录密码，避免会话被他人接管后直接改绑人脸。
 */
public class FaceEnrollBody {

    /** 当前账号的登录密码（二次确认） */
    private String password;

    /** 128 维人脸特征 */
    private List<Double> descriptor;

    /** 录入样本帧数 */
    private Integer samples;

    /** 录入质量分 0-1 */
    private Double quality;

    /** 是否已阅读并同意人脸特征使用说明 */
    private Boolean agree;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Double> getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(List<Double> descriptor) {
        this.descriptor = descriptor;
    }

    public Integer getSamples() {
        return samples;
    }

    public void setSamples(Integer samples) {
        this.samples = samples;
    }

    public Double getQuality() {
        return quality;
    }

    public void setQuality(Double quality) {
        this.quality = quality;
    }

    public Boolean getAgree() {
        return agree;
    }

    public void setAgree(Boolean agree) {
        this.agree = agree;
    }
}
