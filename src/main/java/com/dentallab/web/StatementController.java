package com.dentallab.web;

import com.dentallab.service.StatementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 诊所月结。 */
@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    /** 财务生成诊所月结单。 */
    @PostMapping("/generate")
    public Map<String, Object> generate(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return Web.ok(statementService.generate(Web.user(request),
                Req.longReq(body, "clinicId", "诊所ID"),
                Req.strReq(body, "yearMonth", "账期")));
    }

    /** 诊所确认月结（确认前必须复核全部返工）。 */
    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(statementService.confirm(Web.user(request), id));
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(required = false) Long clinicId) {
        return Web.ok(statementService.list(Web.user(request), clinicId));
    }
}
