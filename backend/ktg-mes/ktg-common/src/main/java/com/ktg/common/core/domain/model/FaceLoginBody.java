package com.ktg.common.core.domain.model;

import java.util.List;

/**
 * 人脸登录请求体。
 *
 * descriptor 为前端 face-api.js 计算出的 128 维特征，不包含照片本身。
 */
public class FaceLoginBody {

    /** 用户账号（登录名） */
    private String username;

    /** 128 维人脸特征 */
    private List<Double> descriptor;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Double> getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(List<Double> descriptor) {
        this.descriptor = descriptor;
    }
}
