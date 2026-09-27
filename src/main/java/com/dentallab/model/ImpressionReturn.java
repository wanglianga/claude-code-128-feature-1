package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 取模缺陷退回单：技工所接收模型发现边缘不清/气泡时退回诊所重取模。
 * 记录缺陷照片、交付延期、患者预约调整与费用责任，诊所确认后由医生确认归档。
 */
@Getter
@Setter
@Entity
@Table(name = "impression_return", indexes = {
        @Index(name = "idx_ir_order", columnList = "order_id"),
        @Index(name = "idx_ir_status", columnList = "status")
})
public class ImpressionReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    /** 关联牙位；为空表示整单模型退回。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DefectType defectType;

    /** 缺陷照片（逗号分隔），上报时必填。 */
    @Column(nullable = false, length = 1000)
    private String defectPhotos;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImpressionReturnStatus status = ImpressionReturnStatus.REPORTED;

    /** 关联的异常事件（上报时自动登记，归档时自动闭环）。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exception_id")
    private OrderException exception;

    /** 费用责任（重取模费用归属），初始待判定。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Responsibility responsibility = Responsibility.PENDING;

    /** 重取模费用（判定责任时核定）。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** 诊所承担金额（结算时加收）。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal clinicShare = BigDecimal.ZERO;

    /** 技工所承担金额（结算时从应付中扣减）。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal labShare = BigDecimal.ZERO;

    /** 重取模前订单预计试戴日期。 */
    private LocalDate oldExpectedTryInDate;

    /** 诊所重取模后服务重新计算的预计试戴日期。 */
    private LocalDate newExpectedTryInDate;

    /** 重取模造成的交付延期天数。 */
    @Column(nullable = false)
    private Integer delayDays = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by")
    private Account reportedBy;

    @Column(nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reimpressed_by")
    private Account reimpressedBy;

    private LocalDateTime reimpressedAt;

    /** 诊所重取模说明（如新口扫文件/快递单号）。 */
    @Column(length = 500)
    private String reimpressionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_confirmed_by")
    private Account clinicConfirmedBy;

    private LocalDateTime clinicConfirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_confirmed_by")
    private Account doctorConfirmedBy;

    private LocalDateTime doctorConfirmedAt;

    private LocalDateTime archivedAt;
}
