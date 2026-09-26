package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 返工记录：按牙位追踪责任归属与费用分摊，诊所需逐条复核后才可对账。 */
@Getter
@Setter
@Entity
@Table(name = "rework_record", indexes = {
        @Index(name = "idx_rework_order", columnList = "order_id"),
        @Index(name = "idx_rework_item", columnList = "tooth_item_id")
})
public class ReworkRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Responsibility responsibility = Responsibility.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReworkStatus status = ReworkStatus.OPEN;

    /** 返工成本（判定责任时核定）。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal reworkCost = BigDecimal.ZERO;

    /** 诊所承担金额。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal clinicShare = BigDecimal.ZERO;

    /** 技工所承担金额（结算时从应付中扣减）。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal labShare = BigDecimal.ZERO;

    /** 诊所对账前复核标记。 */
    @Column(nullable = false)
    private boolean clinicReviewed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Account reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;
}
