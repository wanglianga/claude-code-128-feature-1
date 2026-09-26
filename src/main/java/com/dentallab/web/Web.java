package com.dentallab.web;

import com.dentallab.config.AuthInterceptor;
import com.dentallab.model.Account;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/** 控制器辅助：取当前用户、构造统一响应。 */
public final class Web {

    private Web() {
    }

    public static Account user(HttpServletRequest request) {
        return (Account) request.getAttribute(AuthInterceptor.ATTR_USER);
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        return body;
    }

    public static Map<String, Object> ok() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        return body;
    }
}
