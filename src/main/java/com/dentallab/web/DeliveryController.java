package com.dentallab.web;

import com.dentallab.model.SignResult;
import com.dentallab.service.DeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 配送发货与签收。 */
@RestController
@RequestMapping("/api")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /** 创建配送单并发货（可指定牙位，默认全部可发货牙位）。 */
    @PostMapping("/orders/{orderId}/shipments")
    public Map<String, Object> ship(HttpServletRequest request, @PathVariable Long orderId,
                                    @RequestBody(required = false) Map<String, Object> body) {
        java.time.LocalDateTime eta = body == null ? null : Req.dateTime(body, "estimatedArrivalAt");
        List<Long> itemIds = body == null ? null : Req.strList(body, "itemIds").stream()
                .map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        return Web.ok(deliveryService.ship(Web.user(request), orderId, itemIds, eta));
    }

    /** 签收：GOOD/LATE/DAMAGED，签收结果影响试戴安排。 */
    @PostMapping("/shipments/{id}/sign")
    public Map<String, Object> sign(HttpServletRequest request, @PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        return Web.ok(deliveryService.sign(Web.user(request), id,
                Req.enumReq(body, "signResult", SignResult.class, "签收结果"),
                Req.str(body, "signedByName"),
                Req.str(body, "signNote"),
                Req.str(body, "delayReason")));
    }

    @GetMapping("/orders/{orderId}/shipments")
    public Map<String, Object> listByOrder(@PathVariable Long orderId) {
        return Web.ok(deliveryService.listByOrder(orderId));
    }
}
