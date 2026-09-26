package com.dentallab.web;

import com.dentallab.model.TryInResult;
import com.dentallab.service.TryInService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 试戴结果与患者体验。 */
@RestController
@RequestMapping("/api/orders/{orderId}/try-ins")
public class TryInController {

    private final TryInService tryInService;

    public TryInController(TryInService tryInService) {
        this.tryInService = tryInService;
    }

    /** 记录试戴结果（FIT/MISFIT），可针对单个牙位或整单。 */
    @PostMapping
    public Map<String, Object> record(HttpServletRequest request, @PathVariable Long orderId,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(tryInService.recordResult(Web.user(request), orderId,
                Req.longVal(body, "toothItemId"),
                Req.enumReq(body, "result", TryInResult.class, "试戴结果"),
                Req.str(body, "feedback"),
                Req.intVal(body, "experienceScore")));
    }

    @GetMapping
    public Map<String, Object> list(@PathVariable Long orderId) {
        return Web.ok(tryInService.listByOrder(orderId));
    }
}
