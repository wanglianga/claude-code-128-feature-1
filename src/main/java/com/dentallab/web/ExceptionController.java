package com.dentallab.web;

import com.dentallab.model.ExceptionType;
import com.dentallab.service.ExceptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 异常事件上报与处理。 */
@RestController
@RequestMapping("/api")
public class ExceptionController {

    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    /** 上报订单异常。 */
    @PostMapping("/orders/{orderId}/exceptions")
    public Map<String, Object> report(HttpServletRequest request, @PathVariable Long orderId,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(exceptionService.report(Web.user(request), orderId,
                Req.enumReq(body, "type", ExceptionType.class, "异常类型"),
                Req.longVal(body, "toothItemId"),
                Req.strReq(body, "description", "异常描述")));
    }

    /** 客服/管理员处理异常。 */
    @PostMapping("/exceptions/{id}/handle")
    public Map<String, Object> handle(HttpServletRequest request, @PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(exceptionService.handle(Web.user(request), id,
                Req.strReq(body, "resolution", "处理意见"),
                Req.bool(body, "resolved", true)));
    }

    @GetMapping("/orders/{orderId}/exceptions")
    public Map<String, Object> listByOrder(@PathVariable Long orderId) {
        return Web.ok(exceptionService.listByOrder(orderId));
    }

    /** 待处理异常（客服工作台）。 */
    @GetMapping("/exceptions/open")
    public Map<String, Object> listOpen() {
        return Web.ok(exceptionService.listOpen());
    }
}
