package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.*;
import com.dentallab.repo.DentureOrderRepo;
import com.dentallab.repo.ReworkRecordRepo;
import com.dentallab.repo.ToothItemRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 返工：按牙位记录返工原因与责任归属，责任分摊进入结算，诊所对账前逐条复核。 */
@Service
public class ReworkService {

    private final ReworkRecordRepo reworkRepo;
    private final DentureOrderRepo orderRepo;
    private final ToothItemRepo itemRepo;
    private final AuthService authService;

    public ReworkService(ReworkRecordRepo reworkRepo, DentureOrderRepo orderRepo,
                         ToothItemRepo itemRepo, AuthService authService) {
        this.reworkRepo = reworkRepo;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.authService = authService;
    }

    /** 创建返工单：牙位打回扫描设计环节重做。 */
    @Transactional
    public Map<String, Object> create(Account user, Long orderId, Long toothItemId, String reason) {
        authService.requireRole(user, Role.QC_INSPECTOR, Role.TECHNICIAN, Role.CUSTOMER_SERVICE,
                Role.LAB_ADMIN, Role.CLINIC_DOCTOR, Role.CLINIC_ADMIN);
        DentureOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> BizException.notFound("订单#" + orderId));
        ToothItem item = itemRepo.findById(toothItemId)
                .orElseThrow(() -> BizException.notFound("牙位明细#" + toothItemId));
        if (!item.getOrder().getId().equals(orderId)) {
            throw new BizException("牙位明细#" + toothItemId + "不属于订单#" + orderId);
        }
        List<ReworkRecord> open = reworkRepo.findByToothItemIdAndStatus(toothItemId, ReworkStatus.OPEN);
        if (!open.isEmpty()) {
            throw new BizException("牙位 " + item.getToothPosition() + " 已有进行中的返工单#" + open.get(0).getId());
        }
        ReworkRecord rework = new ReworkRecord();
        rework.setOrder(order);
        rework.setToothItem(item);
        rework.setReason(reason);
        reworkRepo.save(rework);

        // 牙位打回生产线，从模型接收重新流转
        item.setStatus(ToothItemStatus.REWORKING);
        item.setCurrentStage(null);
        item.setUpdatedAt(LocalDateTime.now());
        itemRepo.save(item);
        if (order.getStatus() != OrderStatus.IN_PRODUCTION) {
            order.setStatus(OrderStatus.IN_PRODUCTION);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);
        }
        return Views.rework(rework);
    }

    /** 判定返工责任并核定返工成本，自动计算双方分摊。 */
    @Transactional
    public Map<String, Object> assignResponsibility(Account user, Long reworkId,
                                                    Responsibility responsibility, BigDecimal reworkCost) {
        authService.requireRole(user, Role.LAB_ADMIN, Role.CUSTOMER_SERVICE);
        ReworkRecord rework = reworkRepo.findById(reworkId)
                .orElseThrow(() -> BizException.notFound("返工单#" + reworkId));
        if (responsibility == Responsibility.PENDING) {
            throw new BizException("请将责任判定为 诊所/技工所/双方共担");
        }
        if (reworkCost == null || reworkCost.signum() < 0) {
            throw new BizException("返工成本 reworkCost 必须为非负数");
        }
        rework.setResponsibility(responsibility);
        rework.setReworkCost(reworkCost);
        switch (responsibility) {
            case CLINIC -> {
                rework.setClinicShare(reworkCost);
                rework.setLabShare(BigDecimal.ZERO);
            }
            case LAB -> {
                rework.setClinicShare(BigDecimal.ZERO);
                rework.setLabShare(reworkCost);
            }
            case SHARED -> {
                BigDecimal half = reworkCost.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                rework.setClinicShare(half);
                rework.setLabShare(reworkCost.subtract(half));
            }
            default -> throw new BizException("非法责任类型");
        }
        reworkRepo.save(rework);
        return Views.rework(rework);
    }

    /** 诊所对账前复核返工。 */
    @Transactional
    public Map<String, Object> clinicReview(Account user, Long reworkId) {
        authService.requireRole(user, Role.CLINIC_DOCTOR, Role.CLINIC_ADMIN);
        ReworkRecord rework = reworkRepo.findById(reworkId)
                .orElseThrow(() -> BizException.notFound("返工单#" + reworkId));
        if (user.getClinic() == null
                || !user.getClinic().getId().equals(rework.getOrder().getClinic().getId())) {
            throw BizException.forbidden("只能复核本诊所订单的返工");
        }
        if (rework.getResponsibility() == Responsibility.PENDING) {
            throw new BizException("返工责任尚未判定，诊所不能复核");
        }
        rework.setClinicReviewed(true);
        rework.setReviewedBy(user);
        rework.setReviewedAt(LocalDateTime.now());
        reworkRepo.save(rework);
        return Views.rework(rework);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByOrder(Long orderId) {
        return reworkRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(Views::rework).collect(Collectors.toList());
    }

    /** 诊所待复核返工列表（对账前复核入口）。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUnreviewed(Account clinicUser) {
        authService.requireRole(clinicUser, Role.CLINIC_DOCTOR, Role.CLINIC_ADMIN);
        if (clinicUser.getClinic() == null) {
            throw new BizException("当前账号未关联诊所");
        }
        List<DentureOrder> orders = orderRepo.findByClinicIdOrderByIdDesc(clinicUser.getClinic().getId());
        List<Long> orderIds = orders.stream().map(DentureOrder::getId).collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return reworkRepo.findByOrderIdIn(orderIds).stream()
                .filter(r -> !r.isClinicReviewed())
                .map(Views::rework).collect(Collectors.toList());
    }
}
