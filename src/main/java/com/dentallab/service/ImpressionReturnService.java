package com.dentallab.service;

import com.dentallab.config.BizException;
import com.dentallab.model.*;
import com.dentallab.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 取模缺陷退回：技工所接收模型发现边缘不清/气泡 → 上传缺陷照片退回诊所重取模。
 * 重取模造成的交付延期、患者预约调整与费用责任写入订单；诊所重取后服务重算交付日期，
 * 患者端/医生端同步提醒，诊所端确认，医生确认后归档。退回期间旧模型不得继续排产。
 */
@Service
public class ImpressionReturnService {

    /** 诊所重取模后重新排产的标准周期（天），用于重算交付日期。 */
    static final int REMAKE_LEAD_DAYS = 7;

    private final ImpressionReturnRepo returnRepo;
    private final DentureOrderRepo orderRepo;
    private final ToothItemRepo itemRepo;
    private final OrderExceptionRepo exceptionRepo;
    private final NoticeRepo noticeRepo;
    private final TryInRecordRepo tryInRepo;
    private final AuthService authService;

    public ImpressionReturnService(ImpressionReturnRepo returnRepo, DentureOrderRepo orderRepo,
                                   ToothItemRepo itemRepo, OrderExceptionRepo exceptionRepo,
                                   NoticeRepo noticeRepo, TryInRecordRepo tryInRepo,
                                   AuthService authService) {
        this.returnRepo = returnRepo;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.exceptionRepo = exceptionRepo;
        this.noticeRepo = noticeRepo;
        this.tryInRepo = tryInRepo;
        this.authService = authService;
    }

    /**
     * 技工所上报取模缺陷退回：必须上传缺陷照片，自动通知诊所重取模，
     * 受影响牙位退出排产（旧模型不得继续排产）。
     */
    @Transactional
    public Map<String, Object> report(Account user, Long orderId, Long toothItemId, DefectType defectType,
                                      List<String> defectPhotos, String description) {
        authService.requireRole(user, Role.LAB_RECEIVER, Role.LAB_ADMIN, Role.TECHNICIAN);
        DentureOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> BizException.notFound("订单#" + orderId));
        if (order.getStatus() == OrderStatus.SUBMITTED) {
            throw new BizException("技工所尚未接单接收模型，不能上报取模缺陷");
        }
        if (EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED,
                OrderStatus.DELIVERED, OrderStatus.TRY_IN).contains(order.getStatus())) {
            throw new BizException("订单当前状态[" + order.getStatus().getLabel() + "]不允许取模缺陷退回");
        }
        if (defectPhotos == null || defectPhotos.isEmpty()) {
            throw new BizException("取模缺陷退回必须上传缺陷照片 defectPhotos（边缘不清/气泡照片）");
        }
        ToothItem item = null;
        if (toothItemId != null) {
            item = itemRepo.findById(toothItemId)
                    .orElseThrow(() -> BizException.notFound("牙位明细#" + toothItemId));
            if (!item.getOrder().getId().equals(orderId)) {
                throw new BizException("牙位明细#" + toothItemId + "不属于订单#" + orderId);
            }
        }
        // 同一范围只允许一张未归档退回单
        for (ImpressionReturn active : activeReturns(orderId)) {
            if (active.getToothItem() == null || item == null
                    || active.getToothItem().getId().equals(item.getId())) {
                throw new BizException("订单已存在未归档的取模退回单#" + active.getId()
                        + "，请先完成重取模与归档流程");
            }
        }

        ImpressionReturn ret = new ImpressionReturn();
        ret.setOrder(order);
        ret.setToothItem(item);
        ret.setDefectType(defectType);
        ret.setDefectPhotos(String.join(",", defectPhotos));
        ret.setDescription(description);
        ret.setReportedBy(user);
        ret.setOldExpectedTryInDate(order.getExpectedTryInDate());

        // 自动登记异常事件，把各方连接到同一颗牙
        OrderException event = new OrderException();
        event.setOrder(order);
        event.setToothItem(item);
        event.setType(ExceptionType.IMPRESSION_DEFECT);
        event.setDescription("取模缺陷退回（" + defectType.getLabel()
                + (item != null ? "，牙位 " + item.getToothPosition() : "，整单模型") + "）: "
                + (description == null || description.isBlank() ? "要求诊所重新取模" : description));
        event.setReportedBy(user);
        exceptionRepo.save(event);
        ret.setException(event);
        returnRepo.save(ret);

        // 受影响牙位退出排产：旧模型不再继续流转，待诊所重取后从模型接收重新开始
        for (ToothItem t : affectedItems(order, item)) {
            t.setStatus(ToothItemStatus.IMPRESSION_RETURNED);
            t.setCurrentStage(null);
            t.setUpdatedAt(LocalDateTime.now());
            itemRepo.save(t);
        }
        order.setStatus(OrderStatus.IN_PRODUCTION);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        // 通知诊所重取模
        notify(ret, NoticeAudience.CLINIC, NoticeType.REIMPRESSION_REQUEST,
                "订单 " + order.getOrderNo() + " 取模缺陷（" + defectType.getLabel()
                        + (item != null ? "，牙位 " + item.getToothPosition() : "") + "），"
                        + "技工所已退回模型并停止排产，请重新取模后回传；缺陷照片: " + ret.getDefectPhotos());
        return Views.impressionReturn(ret);
    }

    /**
     * 诊所重新取模：服务重新计算交付日期（预计试戴日期 = 今天 + 重排周期），
     * 交付延期与患者预约调整写入订单，患者端/医生端同步提醒。
     */
    @Transactional
    public Map<String, Object> reimpress(Account user, Long returnId, String note) {
        ImpressionReturn ret = getReturn(returnId);
        requireClinicSide(user, ret, "重新取模");
        if (ret.getStatus() != ImpressionReturnStatus.REPORTED) {
            throw new BizException("退回单当前状态[" + ret.getStatus().getLabel() + "]，不能重复登记重取模");
        }
        DentureOrder order = ret.getOrder();
        LocalDate today = LocalDate.now();
        LocalDate newDate = today.plusDays(REMAKE_LEAD_DAYS);
        LocalDate oldDate = order.getExpectedTryInDate() != null ? order.getExpectedTryInDate() : today;
        int delayDays = (int) Math.max(0, ChronoUnit.DAYS.between(oldDate, newDate));

        // 交付延期写入订单
        ret.setOldExpectedTryInDate(order.getExpectedTryInDate());
        ret.setNewExpectedTryInDate(newDate);
        ret.setDelayDays(delayDays);
        order.setExpectedTryInDate(newDate);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        // 患者预约调整写入订单：未执行的试戴安排按延期天数顺延
        if (delayDays > 0) {
            for (TryInRecord appointment : tryInRepo.findByOrderIdOrderByIdAsc(order.getId())) {
                if (appointment.getResult() == null && appointment.getScheduledAt() != null) {
                    appointment.setScheduledAt(appointment.getScheduledAt().plusDays(delayDays));
                    tryInRepo.save(appointment);
                }
            }
        }

        // 新模型可以重新接收：牙位回到待接收，从模型接收重新流转
        for (ToothItem t : affectedItems(order, ret.getToothItem())) {
            if (t.getStatus() == ToothItemStatus.IMPRESSION_RETURNED) {
                t.setStatus(ToothItemStatus.PENDING);
                t.setUpdatedAt(LocalDateTime.now());
                itemRepo.save(t);
            }
        }

        ret.setReimpressedBy(user);
        ret.setReimpressedAt(LocalDateTime.now());
        ret.setReimpressionNote(note);
        ret.setStatus(ImpressionReturnStatus.REIMPRESSED);
        returnRepo.save(ret);

        // 患者端同步提醒（预约调整）+ 医生端同步新日期
        notify(ret, NoticeAudience.PATIENT, NoticeType.APPOINTMENT_ADJUSTMENT,
                "患者 " + order.getPatientCode() + "：因诊所重新取模，订单 " + order.getOrderNo()
                        + " 试戴预约调整为 " + newDate + "（延期 " + delayDays + " 天），请按新时间到诊。");
        notify(ret, NoticeAudience.DOCTOR, NoticeType.DELIVERY_DATE_SYNC,
                "订单 " + order.getOrderNo() + " 诊所已重新取模，交付/预计试戴日期同步为 " + newDate
                        + "（原 " + oldDate + "，延期 " + delayDays + " 天）。");
        return Views.impressionReturn(ret);
    }

    /** 判定重取模费用责任并核定费用，自动计算双方分摊（进入订单结算；归档后仍可判定）。 */
    @Transactional
    public Map<String, Object> assignResponsibility(Account user, Long returnId,
                                                    Responsibility responsibility, BigDecimal cost) {
        authService.requireRole(user, Role.LAB_ADMIN, Role.CUSTOMER_SERVICE);
        ImpressionReturn ret = getReturn(returnId);
        if (responsibility == Responsibility.PENDING) {
            throw new BizException("请将费用责任判定为 诊所/技工所/双方共担");
        }
        if (cost == null || cost.signum() < 0) {
            throw new BizException("重取模费用 cost 必须为非负数");
        }
        ret.setResponsibility(responsibility);
        ret.setCost(cost);
        switch (responsibility) {
            case CLINIC -> {
                ret.setClinicShare(cost);
                ret.setLabShare(BigDecimal.ZERO);
            }
            case LAB -> {
                ret.setClinicShare(BigDecimal.ZERO);
                ret.setLabShare(cost);
            }
            case SHARED -> {
                BigDecimal half = cost.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                ret.setClinicShare(half);
                ret.setLabShare(cost.subtract(half));
            }
            default -> throw new BizException("非法责任类型");
        }
        returnRepo.save(ret);
        return Views.impressionReturn(ret);
    }

    /** 诊所端确认（确认新交付日期与预约调整）。 */
    @Transactional
    public Map<String, Object> clinicConfirm(Account user, Long returnId) {
        ImpressionReturn ret = getReturn(returnId);
        requireClinicSide(user, ret, "确认退回单");
        if (ret.getStatus() != ImpressionReturnStatus.REIMPRESSED) {
            throw new BizException("退回单当前状态[" + ret.getStatus().getLabel() + "]，需诊所重取模后再确认");
        }
        ret.setClinicConfirmedBy(user);
        ret.setClinicConfirmedAt(LocalDateTime.now());
        ret.setStatus(ImpressionReturnStatus.CLINIC_CONFIRMED);
        returnRepo.save(ret);
        return Views.impressionReturn(ret);
    }

    /** 医生确认后归档：退回流程闭环，关联异常自动解决。 */
    @Transactional
    public Map<String, Object> doctorConfirm(Account user, Long returnId) {
        authService.requireRole(user, Role.CLINIC_DOCTOR);
        ImpressionReturn ret = getReturn(returnId);
        requireClinicSide(user, ret, "确认归档");
        if (ret.getStatus() != ImpressionReturnStatus.CLINIC_CONFIRMED) {
            throw new BizException("退回单当前状态[" + ret.getStatus().getLabel() + "]，需诊所端确认后再由医生确认归档");
        }
        ret.setDoctorConfirmedBy(user);
        ret.setDoctorConfirmedAt(LocalDateTime.now());
        ret.setArchivedAt(LocalDateTime.now());
        ret.setStatus(ImpressionReturnStatus.ARCHIVED);
        returnRepo.save(ret);

        OrderException event = ret.getException();
        if (event != null && event.getStatus() != ExceptionStatus.RESOLVED) {
            event.setStatus(ExceptionStatus.RESOLVED);
            event.setHandledBy(user);
            event.setHandledAt(LocalDateTime.now());
            event.setResolution("诊所已重新取模，交付日期重算延期 " + ret.getDelayDays()
                    + " 天，诊所端已确认，医生确认归档");
            exceptionRepo.save(event);
        }
        return Views.impressionReturn(ret);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByOrder(Long orderId) {
        return returnRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(Views::impressionReturn).collect(Collectors.toList());
    }

    /** 未归档退回单工作台：诊所看本诊所，技工所/客服看全部。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOpen(Account user) {
        boolean clinicSide = user.getRole() == Role.CLINIC_DOCTOR || user.getRole() == Role.CLINIC_ADMIN;
        List<ImpressionReturn> open = returnRepo.findByStatusInOrderByIdDesc(
                EnumSet.of(ImpressionReturnStatus.REPORTED, ImpressionReturnStatus.REIMPRESSED,
                        ImpressionReturnStatus.CLINIC_CONFIRMED));
        return open.stream()
                .filter(r -> !clinicSide || (user.getClinic() != null
                        && r.getOrder().getClinic().getId().equals(user.getClinic().getId())))
                .map(Views::impressionReturn).collect(Collectors.toList());
    }

    // ---- 内部逻辑 ----

    private ImpressionReturn getReturn(Long returnId) {
        return returnRepo.findById(returnId)
                .orElseThrow(() -> BizException.notFound("取模退回单#" + returnId));
    }

    /** 未归档（处理中）的退回单。 */
    private List<ImpressionReturn> activeReturns(Long orderId) {
        return returnRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                .filter(r -> r.getStatus() != ImpressionReturnStatus.ARCHIVED)
                .collect(Collectors.toList());
    }

    private List<ToothItem> affectedItems(DentureOrder order, ToothItem item) {
        return item != null ? List.of(item) : itemRepo.findByOrderIdOrderByIdAsc(order.getId());
    }

    private void requireClinicSide(Account user, ImpressionReturn ret, String action) {
        authService.requireRole(user, Role.CLINIC_DOCTOR, Role.CLINIC_ADMIN);
        if (user.getClinic() == null
                || !user.getClinic().getId().equals(ret.getOrder().getClinic().getId())) {
            throw BizException.forbidden("只能" + action + "本诊所订单的退回单");
        }
    }

    private void notify(ImpressionReturn ret, NoticeAudience audience, NoticeType type, String content) {
        Notice notice = new Notice();
        notice.setOrder(ret.getOrder());
        notice.setImpressionReturn(ret);
        notice.setAudience(audience);
        notice.setType(type);
        notice.setContent(content);
        noticeRepo.save(notice);
    }
}
