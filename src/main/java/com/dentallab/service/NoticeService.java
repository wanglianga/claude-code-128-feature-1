package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.*;
import com.dentallab.repo.DentureOrderRepo;
import com.dentallab.repo.NoticeRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 各端同步通知查询：诊所端、医生端、患者端。 */
@Service
public class NoticeService {

    private final NoticeRepo noticeRepo;
    private final DentureOrderRepo orderRepo;

    public NoticeService(NoticeRepo noticeRepo, DentureOrderRepo orderRepo) {
        this.noticeRepo = noticeRepo;
        this.orderRepo = orderRepo;
    }

    /** 按接收端查询通知；诊所角色只能看本诊所订单的通知。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Account user, NoticeAudience audience, Long orderId) {
        if (audience == null) {
            throw new BizException("请指定接收端 audience（CLINIC/DOCTOR/PATIENT）");
        }
        boolean clinicSide = user.getRole() == Role.CLINIC_DOCTOR || user.getRole() == Role.CLINIC_ADMIN;
        List<Notice> notices;
        if (orderId != null) {
            DentureOrder order = orderRepo.findById(orderId)
                    .orElseThrow(() -> BizException.notFound("订单#" + orderId));
            if (clinicSide && (user.getClinic() == null
                    || !user.getClinic().getId().equals(order.getClinic().getId()))) {
                throw BizException.forbidden("只能查看本诊所订单的通知");
            }
            notices = noticeRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                    .filter(n -> n.getAudience() == audience)
                    .collect(Collectors.toList());
        } else if (clinicSide) {
            if (user.getClinic() == null) {
                throw new BizException("当前账号未关联诊所");
            }
            List<Long> orderIds = orderRepo.findByClinicIdOrderByIdDesc(user.getClinic().getId())
                    .stream().map(DentureOrder::getId).collect(Collectors.toList());
            notices = orderIds.isEmpty() ? List.of()
                    : noticeRepo.findByAudienceAndOrderIdInOrderByIdDesc(audience, orderIds);
        } else {
            notices = noticeRepo.findByAudienceOrderByIdDesc(audience);
        }
        return notices.stream().map(Views::notice).collect(Collectors.toList());
    }
}
