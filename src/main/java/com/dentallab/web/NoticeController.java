package com.dentallab.web;

import com.dentallab.model.NoticeAudience;
import com.dentallab.service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 各端同步通知查询（诊所端/医生端/患者端）。 */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** 按接收端查询同步通知，可按订单过滤。 */
    @GetMapping
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(required = false) String audience,
                                    @RequestParam(required = false) Long orderId) {
        Map<String, Object> probe = new HashMap<>();
        probe.put("audience", audience);
        return Web.ok(noticeService.list(Web.user(request),
                Req.enumReq(probe, "audience", NoticeAudience.class, "接收端"), orderId));
    }
}
