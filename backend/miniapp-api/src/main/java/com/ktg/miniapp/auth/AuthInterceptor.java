package com.ktg.miniapp.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 鉴权拦截器：校验 Authorization: Bearer <token>
 * 未通过时直接返回 HTTP 401 + {code:401,msg:...}，前端据此跳转登录页。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String UNAUTHORIZED_BODY = "{\"code\":401,\"msg\":\"登录已过期，请重新登录\",\"data\":null}";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        Map<String, Object> payload = tokenService.verify(token);
        if (payload == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(UNAUTHORIZED_BODY);
            return false;
        }
        request.setAttribute("miniappAccount", payload);
        // 令牌里的 cid 即企业客户ID，后续数据层按该客户隔离
        Object clientId = payload.get("cid");
        ClientContext.set(clientId instanceof Number ? ((Number) clientId).longValue() : null);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 线程复用，请求结束必须清理，避免串到下一个客户
        ClientContext.clear();
    }
}
