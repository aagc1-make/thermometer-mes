package com.ktg.miniapp.common;

/** 业务异常，统一由 GlobalExceptionHandler 转为 { code:500, msg } */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
