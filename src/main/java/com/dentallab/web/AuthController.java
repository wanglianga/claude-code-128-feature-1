package com.dentallab.web;

import com.dentallab.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String username = Req.strReq(body, "username", "用户名");
        String password = Req.strReq(body, "password", "密码");
        return Web.ok(authService.login(username, password));
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return Web.ok(com.dentallab.service.Views.account(Web.user(request)));
    }
}
