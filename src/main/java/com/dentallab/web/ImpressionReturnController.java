package com.dentallab.web;

import com.dentallab.model.DefectType;
import com.dentallab.model.Responsibility;
import com.dentallab.service.ImpressionReturnService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 取模缺陷退回：上报（附缺陷照片）、诊所重取模、诊所端确认、医生归档、多端通知查询。 */
@RestController
@RequestMapping("/api")
public class ImpressionReturnController {

    private final ImpressionReturnService impressionReturnService;

    public ImpressionReturnController(ImpressionReturnService impressionReturnService) {
        this.impressionReturnService = impressionReturnService;
    }

    /** 技工所上报取模缺陷退回（必须上传缺陷照片）。 */
    @PostMapping("/orders/{orderId}/impression-returns")
    public Map<String, Object> report(HttpServletRequest request, @PathVariable Long orderId,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(impressionReturnService.report(Web.user(request), orderId,
                Req.longVal(body, "toothItemId"),
                Req.enumReq(body, "defectType", DefectType.class, "缺陷类型"),
                Req.strList(body, "defectPhotos"),
                Req.str(body, "description"),
                Req.intVal(body, "delayDays"),
                Req.enumVal(body, "costResponsibility", Responsibility.class),
                Req.decimal(body, "costAmount")));
    }

    /** 诊所提交重新取模：服务重算交付日期并同步患者/医生/诊所三端。 */
    @PostMapping("/impression-returns/{id}/reimpression")
    public Map<String, Object> reimpression(HttpServletRequest request, @PathVariable Long id,
                                            @RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? null : Req.str(body, "note");
        return Web.ok(impressionReturnService.reimpression(Web.user(request), id, note));
    }

    /** 诊所端确认新交付安排。 */
    @PostMapping("/impression-returns/{id}/clinic-confirm")
    public Map<String, Object> clinicConfirm(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(impressionReturnService.clinicConfirm(Web.user(request), id));
    }

    /** 医生确认后归档。 */
    @PostMapping("/impression-returns/{id}/doctor-archive")
    public Map<String, Object> doctorArchive(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(impressionReturnService.doctorArchive(Web.user(request), id));
    }

    /** 订单的取模退回单列表。 */
    @GetMapping("/orders/{orderId}/impression-returns")
    public Map<String, Object> listByOrder(@PathVariable Long orderId) {
        return Web.ok(impressionReturnService.listByOrder(orderId));
    }

    /** 订单的多端同步通知（患者端/医生端/诊所端）。 */
    @GetMapping("/orders/{orderId}/notifications")
    public Map<String, Object> listNotifications(@PathVariable Long orderId) {
        return Web.ok(impressionReturnService.listNotifications(orderId));
    }
}
