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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 取模缺陷退回：技工所接收模型发现边缘不清/气泡时上报（必须上传缺陷照片），
 * 通知诊所重取模；交付延期、患者预约调整与费用责任写入订单；
 * 诊所重取模后重算交付日期并同步患者/医生/诊所三端，诊所确认、医生归档。
 * 退回期间旧模型不得继续排产。
 */
@Service
public class ImpressionReturnService {

    private static final int DEFAULT_DELAY_DAYS = 7;

    private final ImpressionReturnRepo returnRepo;
    private final NotificationRepo notificationRepo;
    private final DentureOrderRepo orderRepo;
    private final ToothItemRepo itemRepo;
    private final AuthService authService;

    public ImpressionReturnService(ImpressionReturnRepo returnRepo, NotificationRepo notificationRepo,
                                   DentureOrderRepo orderRepo, ToothItemRepo itemRepo,
                                   AuthService authService) {
        this.returnRepo = returnRepo;
        this.notificationRepo = notificationRepo;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.authService = authService;
    }

    /**
     * 技工所上报取模缺陷退回：必须上传缺陷照片；受影响牙位打回“取模缺陷退回”，
     * 旧模型不再排产；交付延期与费用责任写入订单；通知诊所重取模。
     */
    @Transactional
    public Map<String, Object> report(Account labUser, Long orderId, Long toothItemId, DefectType defectType,
                                      List<String> photos, String description, Integer delayDays,
                                      Responsibility costResponsibility, BigDecimal costAmount) {
        authService.requireRole(labUser, Role.LAB_RECEIVER, Role.LAB_ADMIN, Role.TECHNICIAN);
        DentureOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> BizException.notFound("订单#" + orderId));
        if (!EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.IN_PRODUCTION).contains(order.getStatus())) {
            throw new BizException("订单当前状态[" + order.getStatus().getLabel()
                    + "]不能登记取模缺陷退回（需已接单且未发货）");
        }
        if (photos == null || photos.isEmpty()) {
            throw new BizException("取模缺陷退回必须上传缺陷照片 defectPhotos");
        }
        int delay = delayDays == null ? DEFAULT_DELAY_DAYS : delayDays;
        if (delay < 1 || delay > 60) {
            throw new BizException("交付延期天数 delayDays 必须在 1-60 之间");
        }
        Responsibility responsibility = costResponsibility == null ? Responsibility.CLINIC : costResponsibility;
        if (responsibility == Responsibility.PENDING) {
            throw new BizException("费用责任需明确为 诊所/技工所/双方共担");
        }
        BigDecimal amount = costAmount == null ? BigDecimal.ZERO : costAmount;
        if (amount.signum() < 0) {
            throw new BizException("重取模费用 costAmount 必须为非负数");
        }

        ToothItem item = null;
        if (toothItemId != null) {
            item = itemRepo.findById(toothItemId)
                    .orElseThrow(() -> BizException.notFound("牙位明细#" + toothItemId));
            if (!item.getOrder().getId().equals(orderId)) {
                throw new BizException("牙位明细#" + toothItemId + "不属于订单#" + orderId);
            }
        }

        ImpressionReturn record = new ImpressionReturn();
        record.setOrder(order);
        record.setToothItem(item);
        record.setDefectType(defectType);
        record.setDefectPhotos(String.join(",", photos));
        record.setDescription(description);
        record.setReportedBy(labUser);
        record.setDelayDays(delay);
        record.setCostResponsibility(responsibility);
        record.setCostAmount(amount);
        applyShares(record, responsibility, amount);
        record.setOldExpectedTryInDate(order.getExpectedTryInDate());
        returnRepo.save(record);

        // 旧模型不再排产：受影响牙位打回，清空工序进度
        for (ToothItem t : affectedItems(order, item)) {
            t.setStatus(ToothItemStatus.IMPRESSION_RETURNED);
            t.setCurrentStage(null);
            t.setUpdatedAt(LocalDateTime.now());
            itemRepo.save(t);
        }
        order.setStatus(OrderStatus.IMPRESSION_RETURNED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        notify(order, record, RecipientType.CLINIC,
                "订单" + order.getOrderNo() + "取模缺陷退回（" + defectType.getLabel()
                        + (item != null ? "，牙位 " + item.getToothPosition() : "，整单")
                        + "）：请诊所重新取模并寄送新模型；预计交付延期 " + delay + " 天。");
        return Views.impressionReturn(record);
    }

    /**
     * 诊所重新取模：服务重新计算交付日期（预计试戴日期顺延延期天数），
     * 患者端同步预约调整提醒、医生端同步新日期、诊所端待确认；牙位回到待接收，等待新模型。
     */
    @Transactional
    public Map<String, Object> reimpression(Account clinicUser, Long returnId, String note) {
        ImpressionReturn record = getReturn(returnId);
        requireClinicSide(clinicUser, record, "重新取模");
        if (record.getStatus() != ImpressionReturnStatus.PENDING_REIMPRESSION) {
            throw new BizException("当前状态[" + record.getStatus().getLabel() + "]不能提交重新取模");
        }
        DentureOrder order = record.getOrder();

        // 重新计算交付日期：以原预计试戴日期（已过期的按今天）为基准顺延延期天数
        LocalDate oldDate = record.getOldExpectedTryInDate();
        LocalDate base = oldDate != null && oldDate.isAfter(LocalDate.now()) ? oldDate : LocalDate.now();
        LocalDate newDate = base.plusDays(record.getDelayDays());
        record.setNewExpectedTryInDate(newDate);
        record.setReimpressionNote(note);
        record.setReimpressionAt(LocalDateTime.now());
        record.setStatus(ImpressionReturnStatus.REIMPRESSION_SUBMITTED);
        record.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(record);

        // 新模型等待重新接收：牙位回到待接收，从模型接收重新流转
        for (ToothItem t : returnedItems(order, record)) {
            t.setStatus(ToothItemStatus.PENDING);
            t.setCurrentStage(null);
            t.setUpdatedAt(LocalDateTime.now());
            itemRepo.save(t);
        }
        order.setExpectedTryInDate(newDate);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        notify(order, record, RecipientType.PATIENT,
                "【患者提醒】订单" + order.getOrderNo() + "因取模缺陷需重新取模，您的试戴预约由 "
                        + formatDate(oldDate) + " 调整至 " + newDate + "，请按新时间到诊。");
        notify(order, record, RecipientType.DOCTOR,
                "【医生端同步】订单" + order.getOrderNo() + "诊所已重新取模，预计试戴/交付日期已更新为 "
                        + newDate + "（延期 " + record.getDelayDays() + " 天）。");
        notify(order, record, RecipientType.CLINIC,
                "【诊所确认】订单" + order.getOrderNo() + "新交付日期 " + newDate + " 已生成，请诊所端确认。");
        return Views.impressionReturn(record);
    }

    /** 诊所端确认新交付安排。 */
    @Transactional
    public Map<String, Object> clinicConfirm(Account clinicUser, Long returnId) {
        ImpressionReturn record = getReturn(returnId);
        requireClinicSide(clinicUser, record, "确认");
        if (record.getStatus() != ImpressionReturnStatus.REIMPRESSION_SUBMITTED) {
            throw new BizException("当前状态[" + record.getStatus().getLabel() + "]不能确认（需先完成重新取模）");
        }
        record.setStatus(ImpressionReturnStatus.CLINIC_CONFIRMED);
        record.setClinicConfirmedBy(clinicUser);
        record.setClinicConfirmedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(record);
        return Views.impressionReturn(record);
    }

    /** 医生确认后归档。 */
    @Transactional
    public Map<String, Object> doctorArchive(Account doctor, Long returnId) {
        authService.requireRole(doctor, Role.CLINIC_DOCTOR);
        ImpressionReturn record = getReturn(returnId);
        if (doctor.getClinic() == null
                || !doctor.getClinic().getId().equals(record.getOrder().getClinic().getId())) {
            throw BizException.forbidden("只能归档本诊所订单的取模退回单");
        }
        if (record.getStatus() != ImpressionReturnStatus.CLINIC_CONFIRMED) {
            throw new BizException("当前状态[" + record.getStatus().getLabel() + "]不能归档（需诊所端先确认）");
        }
        record.setStatus(ImpressionReturnStatus.ARCHIVED);
        record.setDoctorArchivedBy(doctor);
        record.setDoctorArchivedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        returnRepo.save(record);
        return Views.impressionReturn(record);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByOrder(Long orderId) {
        return returnRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(Views::impressionReturn).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listNotifications(Long orderId) {
        return notificationRepo.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(Views::notification).collect(Collectors.toList());
    }

    // ---- 内部逻辑 ----

    private ImpressionReturn getReturn(Long returnId) {
        return returnRepo.findById(returnId)
                .orElseThrow(() -> BizException.notFound("取模退回单#" + returnId));
    }

    private void requireClinicSide(Account user, ImpressionReturn record, String action) {
        authService.requireRole(user, Role.CLINIC_DOCTOR, Role.CLINIC_ADMIN);
        if (user.getClinic() == null
                || !user.getClinic().getId().equals(record.getOrder().getClinic().getId())) {
            throw BizException.forbidden("只能对本诊所订单的取模退回单执行" + action);
        }
    }

    /** 上报时受影响的牙位：指定牙位或整单未发货牙位。 */
    private List<ToothItem> affectedItems(DentureOrder order, ToothItem item) {
        if (item != null) {
            return List.of(item);
        }
        return itemRepo.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .filter(t -> !EnumSet.of(ToothItemStatus.SHIPPED, ToothItemStatus.DELIVERED,
                        ToothItemStatus.TRY_IN_OK).contains(t.getStatus()))
                .collect(Collectors.toList());
    }

    /** 重取模时需要回到待接收的牙位：本次退回打回的牙位。 */
    private List<ToothItem> returnedItems(DentureOrder order, ImpressionReturn record) {
        if (record.getToothItem() != null) {
            return List.of(record.getToothItem());
        }
        return itemRepo.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .filter(t -> t.getStatus() == ToothItemStatus.IMPRESSION_RETURNED)
                .collect(Collectors.toList());
    }

    private void applyShares(ImpressionReturn record, Responsibility responsibility, BigDecimal amount) {
        switch (responsibility) {
            case CLINIC -> {
                record.setClinicShare(amount);
                record.setLabShare(BigDecimal.ZERO);
            }
            case LAB -> {
                record.setClinicShare(BigDecimal.ZERO);
                record.setLabShare(amount);
            }
            case SHARED -> {
                BigDecimal half = amount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                record.setClinicShare(half);
                record.setLabShare(amount.subtract(half));
            }
            default -> throw new BizException("非法费用责任类型");
        }
    }

    private void notify(DentureOrder order, ImpressionReturn record, RecipientType recipient, String message) {
        Notification notification = new Notification();
        notification.setOrder(order);
        notification.setImpressionReturn(record);
        notification.setRecipientType(recipient);
        notification.setMessage(message);
        notificationRepo.save(notification);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "未设定" : date.toString();
    }
}
