package com.dentallab.web;

import com.dentallab.model.StageType;
import com.dentallab.service.ProductionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 生产工序、医生确认、在线批注、按牙位看板。 */
@RestController
@RequestMapping("/api/orders/{orderId}")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    /** 登记一道工序（操作人=当前登录人）。 */
    @PostMapping("/items/{itemId}/stages")
    public Map<String, Object> recordStage(HttpServletRequest request,
                                           @PathVariable Long orderId, @PathVariable Long itemId,
                                           @RequestBody Map<String, Object> body) {
        return Web.ok(productionService.recordStage(Web.user(request), orderId, itemId,
                Req.enumReq(body, "stage", StageType.class, "工序"),
                Req.longVal(body, "materialBatchId"),
                Req.decimal(body, "materialQty"),
                Req.strList(body, "photos"),
                Req.str(body, "note"),
                Req.dateTime(body, "estimatedFinishAt"),
                Req.bool(body, "finish", true),
                Req.str(body, "designSummary")));
    }

    /** 诊所医生确认设计。 */
    @PostMapping("/items/{itemId}/confirm")
    public Map<String, Object> confirm(HttpServletRequest request,
                                       @PathVariable Long orderId, @PathVariable Long itemId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? null : Req.str(body, "note");
        java.time.LocalDateTime eta = body == null ? null : Req.dateTime(body, "estimatedFinishAt");
        return Web.ok(productionService.confirmDesign(Web.user(request), orderId, itemId, note, eta));
    }

    /** 医生在线批注。 */
    @PostMapping("/items/{itemId}/annotations")
    public Map<String, Object> annotate(HttpServletRequest request,
                                        @PathVariable Long orderId, @PathVariable Long itemId,
                                        @RequestBody Map<String, Object> body) {
        return Web.ok(productionService.annotate(Web.user(request), orderId, itemId,
                Req.strReq(body, "content", "批注内容")));
    }

    /** 按牙位看板：设计、制作、质检、配送、试戴、返工责任。 */
    @GetMapping("/tooth-board")
    public Map<String, Object> toothBoard(@PathVariable Long orderId) {
        return Web.ok(productionService.toothBoard(orderId));
    }
}
