package com.dentallab.web;

import com.dentallab.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 订单结算。 */
@RestController
@RequestMapping("/api")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /** 财务生成订单结算单。 */
    @PostMapping("/orders/{orderId}/settlement")
    public Map<String, Object> generate(HttpServletRequest request, @PathVariable Long orderId) {
        return Web.ok(settlementService.generate(Web.user(request), orderId));
    }

    @GetMapping("/orders/{orderId}/settlement")
    public Map<String, Object> get(@PathVariable Long orderId) {
        return Web.ok(settlementService.getByOrder(orderId));
    }
}
