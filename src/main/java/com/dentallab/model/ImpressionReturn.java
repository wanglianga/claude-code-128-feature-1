package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 取模缺陷退回单：技工所接收模型发现边缘不清/气泡等缺陷时登记，
 * 跟踪诊所重取模、交付延期、患者预约调整与费用责任，直至医生确认归档。
 */
@Getter
@Setter
@Entity
@Table(name = "impression_return", indexes = @Index(name = "idx_impr_order", columnList = "order_id"))
public class ImpressionReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    /** 关联牙位；整单退回为空。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DefectType defectType;

    /** 缺陷照片（必填，逗号分隔），无照片不允许退回。 */
    @Column(nullable = false, length = 2000)
    private String defectPhotos;

    @Column(length = 1000)
    private String description;

    /** 上报人（技工所）。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by")
    private Account reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImpressionReturnStatus status = ImpressionReturnStatus.PENDING_REIMPRESSION;

    /** 重取模造成的交付延期天数。 */
    @Column(nullable = false)
    private Integer delayDays = 7;

    /** 重取模费用责任归属（不允许待判定，上报时即明确）。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Responsibility costResponsibility = Responsibility.CLINIC;

    /** 重取模费用（重寄模型、延期等成本）。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    /** 诊所承担金额（结算时加收）。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal clinicShare = BigDecimal.ZERO;

    /** 技工所承担金额（结算时从应付中扣减）。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal labShare = BigDecimal.ZERO;

    /** 退回前预计试戴日期。 */
    private LocalDate oldExpectedTryInDate;

    /** 诊所重取模后重新计算的预计试戴日期。 */
    private LocalDate newExpectedTryInDate;

    /** 诊所重新取模说明。 */
    @Column(length = 1000)
    private String reimpressionNote;

    private LocalDateTime reimpressionAt;

    /** 诊所端确认人/确认时间。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_confirmed_by")
    private Account clinicConfirmedBy;

    private LocalDateTime clinicConfirmedAt;

    /** 医生确认归档人/归档时间。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_archived_by")
    private Account doctorArchivedBy;

    private LocalDateTime doctorArchivedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
