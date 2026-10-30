package com.ktg.miniapp.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册鉴权拦截器：
 * - /api/v1/auth/login、/api/v1/health 公开
 * - 产品目录、帮助中心为公开只读数据
 * - 其余接口（含订单、询价、地址、对账、发票、客户档案）必须携带有效令牌
 */
@Configuration
public class WebAuthConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebAuthConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/health",
                        "/api/v1/catalog/**",
                        "/api/v1/help/**");
    }
}
