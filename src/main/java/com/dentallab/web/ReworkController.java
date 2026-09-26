package com.dentallab.web;

import com.dentallab.model.Responsibility;
import com.dentallab.service.ReworkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 返工：创建、责任判定、诊所复核。 */
@RestController
@RequestMapping("/api")
public class ReworkController {

    private final ReworkService reworkService;

    public ReworkController(ReworkService reworkService) {
        this.reworkService = reworkService;
    }

    /** 对某个牙位创建返工单。 */
    @PostMapping("/orders/{orderId}/items/{itemId}/reworks")
    public Map<String, Object> create(HttpServletRequest request,
                                      @PathVariable Long orderId, @PathVariable Long itemId,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(reworkService.create(Web.user(request), orderId, itemId,
                Req.strReq(body, "reason", "返工原因")));
    }

    /** 判定返工责任与成本。 */
    @PostMapping("/reworks/{id}/assign-responsibility")
    public Map<String, Object> assign(HttpServletRequest request, @PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(reworkService.assignResponsibility(Web.user(request), id,
                Req.enumReq(body, "responsibility", Responsibility.class, "责任归属"),
                Req.decimal(body, "reworkCost")));
    }

    /** 诊所复核返工（对账前必须全部复核）。 */
    @PostMapping("/reworks/{id}/clinic-review")
    public Map<String, Object> clinicReview(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(reworkService.clinicReview(Web.user(request), id));
    }

    @GetMapping("/orders/{orderId}/reworks")
    public Map<String, Object> listByOrder(@PathVariable Long orderId) {
        return Web.ok(reworkService.listByOrder(orderId));
    }

    /** 本诊所待复核返工。 */
    @GetMapping("/reworks/unreviewed")
    public Map<String, Object> unreviewed(HttpServletRequest request) {
        return Web.ok(reworkService.listUnreviewed(Web.user(request)));
    }
}
