package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单结算单：按难度、返工责任、加急费用与签收结果生成。 */
@Getter
@Setter
@Entity
@Table(name = "settlement", indexes = @Index(name = "idx_settle_order", columnList = "order_id", unique = true))
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private DentureOrder order;

    /** 基础加工费 = 单价 × 牙位数。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    /** 难度系数。 */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal difficultyMultiplier = BigDecimal.ONE;

    /** 加急费用。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rushFee = BigDecimal.ZERO;

    /** 诊所承担的返工费。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal reworkClinicShare = BigDecimal.ZERO;

    /** 技工所承担返工费（从应付中扣减）。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal reworkLabDeduction = BigDecimal.ZERO;

    /** 配送扣罚（延误/破损）。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryPenalty = BigDecimal.ZERO;

    /** 应付总额。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 计算明细说明。 */
    @Column(length = 2000)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();
}
