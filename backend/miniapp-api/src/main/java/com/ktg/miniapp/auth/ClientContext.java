package com.ktg.miniapp.auth;

/**
 * 当前请求登录企业上下文：由鉴权拦截器从令牌写入，数据层按客户隔离。
 * 公开接口（产品目录、帮助中心）没有令牌时回退到演示客户 1。
 */
public final class ClientContext {

    private static final long FALLBACK_CLIENT_ID = 1L;
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private ClientContext() {
    }

    public static void set(Long clientId) {
        if (clientId == null) {
            HOLDER.remove();
        } else {
            HOLDER.set(clientId);
        }
    }

    public static long clientId() {
        Long value = HOLDER.get();
        return value == null ? FALLBACK_CLIENT_ID : value;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
