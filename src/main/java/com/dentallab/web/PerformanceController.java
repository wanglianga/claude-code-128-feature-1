package com.dentallab.web;

import com.dentallab.model.Account;
import com.dentallab.model.Role;
import com.dentallab.service.AuthService;
import com.dentallab.service.PerformanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 技工绩效。 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final AuthService authService;

    public PerformanceController(PerformanceService performanceService, AuthService authService) {
        this.performanceService = performanceService;
        this.authService = authService;
    }

    @GetMapping("/technicians")
    public Map<String, Object> technicians(HttpServletRequest request) {
        Account user = Web.user(request);
        authService.requireRole(user, Role.LAB_ADMIN, Role.FINANCE);
        return Web.ok(performanceService.technicians());
    }
}
