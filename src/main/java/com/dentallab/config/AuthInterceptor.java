package com.dentallab.config;

import com.dentallab.model.Account;
import com.dentallab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 校验 X-Auth-Token，把当前账号放到请求属性 currentUser。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER = "currentUser";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"缺少 X-Auth-Token 请求头\"}");
            return false;
        }
        Account account;
        try {
            account = authService.authenticate(token);
        } catch (BizException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
            return false;
        }
        request.setAttribute(ATTR_USER, account);
        return true;
    }
}
