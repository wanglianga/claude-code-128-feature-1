package com.dentallab.web;

import com.dentallab.model.Account;
import com.dentallab.model.ImpressionMethod;
import com.dentallab.model.ProsthesisType;
import com.dentallab.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 订单：下单、接单、查询、加急、口扫文件。 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 诊所提交订单。 */
    @PostMapping
    public Map<String, Object> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Account user = Web.user(request);
        return Web.ok(orderService.create(
                user,
                Req.strReq(body, "patientCode", "患者匿名编号"),
                Req.enumReq(body, "prosthesisType", ProsthesisType.class, "修复体类型"),
                Req.enumReq(body, "impressionMethod", ImpressionMethod.class, "取模方式"),
                Req.strList(body, "toothPositions"),
                Req.str(body, "biteRelation"),
                Req.str(body, "shade"),
                Req.str(body, "doctorNotes"),
                Req.date(body, "expectedTryInDate"),
                Req.str(body, "materialRequirement"),
                Req.intVal(body, "difficulty")));
    }

    /** 技工所接单。 */
    @PostMapping("/{id}/accept")
    public Map<String, Object> accept(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(orderService.accept(Web.user(request), id));
    }

    /** 患者加急。 */
    @PostMapping("/{id}/rush")
    public Map<String, Object> rush(HttpServletRequest request, @PathVariable Long id,
                                    @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? null : Req.str(body, "reason");
        return Web.ok(orderService.rush(Web.user(request), id, reason));
    }

    /** 登记数字口扫文件。 */
    @PostMapping("/{id}/scan-files")
    public Map<String, Object> addScanFile(HttpServletRequest request, @PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        return Web.ok(orderService.addScanFile(Web.user(request), id,
                Req.strReq(body, "fileName", "文件名"),
                Req.str(body, "format"),
                Req.longVal(body, "sizeBytes"),
                Req.str(body, "storagePath")));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return Web.ok(orderService.detail(id));
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Boolean rush) {
        return Web.ok(orderService.list(Web.user(request), status, rush));
    }
}
