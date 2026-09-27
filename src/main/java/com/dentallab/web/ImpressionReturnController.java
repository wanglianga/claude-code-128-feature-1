package com.dentallab.web;

import com.dentallab.model.DefectType;
import com.dentallab.model.Responsibility;
import com.dentallab.service.ImpressionReturnService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 取模缺陷退回：上报缺陷照片、诊所重取模、费用责任、诊所确认、医生归档。 */
@RestController
@RequestMapping("/api")
public class ImpressionReturnController {

    private final ImpressionReturnService impressionReturnService;

    public ImpressionReturnController(ImpressionReturnService impressionReturnService) {
        this.impressionReturnService = impressionReturnService;
    }

    /** 技工所上报取模缺陷退回（边缘不清/气泡），必须上传缺陷照片。 */
    @PostMapping("/orders/{orderId}/impression-returns")
    public Map<String, Object> report(HttpServletRequest request, @PathVariable Long orderId,
                                      @RequestBody Map<String, Object> body) {
        return Web.ok(impressionReturnService.report(Web.user(request), orderId,
                Req.longVal(body, "toothItemId"),
                Req.enumReq(body, "defectType", DefectType.class, "缺陷类型"),
                Req.strList(body, "defectPhotos"),
                Req.str(body, "description")));
    }

    /** 诊所重新取模：服务重算交付日期并同步患者端/医生端。 */
    @PostMapping("/impression-returns/{id}/reimpress")
    public Map<String, Object> reimpress(HttpServletRequest request, @PathVariable Long id,
                                         @RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? null : Req.str(body, "note");
        return Web.ok(impressionReturnService.reimpress(Web.user(request), id, note));
    }

    /** 判定重取模费用责任与费用。 */
    @PostMapping("/impression-returns/{id}/assign-responsibility")
    public Map<String, Object> assignResponsibility(HttpServletRequest request, @PathVariable Long id,
                                                    @RequestBody Map<String, Object> body) {
        return Web.ok(impressionReturnService.assignResponsibility(Web.user(request), id,
                Req.enumReq(body, "responsibility", Responsibility.class, "费用责任"),
                Req.decimal(body, "cost")));
    }

    /** 诊所端确认。 */
    @PostMapping("/impression-returns/{id}/clinic-confirm")
    public Map<String, Object> clinicConfirm(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(impressionReturnService.clinicConfirm(Web.user(request), id));
    }

    /** 医生确认后归档。 */
    @PostMapping("/impression-returns/{id}/doctor-confirm")
    public Map<String, Object> doctorConfirm(HttpServletRequest request, @PathVariable Long id) {
        return Web.ok(impressionReturnService.doctorConfirm(Web.user(request), id));
    }

    /** 订单的取模退回单列表。 */
    @GetMapping("/orders/{orderId}/impression-returns")
    public Map<String, Object> listByOrder(@PathVariable Long orderId) {
        return Web.ok(impressionReturnService.listByOrder(orderId));
    }

    /** 未归档退回单工作台。 */
    @GetMapping("/impression-returns/open")
    public Map<String, Object> listOpen(HttpServletRequest request) {
        return Web.ok(impressionReturnService.listOpen(Web.user(request)));
    }
}
