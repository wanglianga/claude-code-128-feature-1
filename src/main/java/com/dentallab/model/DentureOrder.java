package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 义齿加工订单主表。 */
@Getter
@Setter
@Entity
@Table(name = "denture_order", indexes = {
        @Index(name = "idx_order_no", columnList = "order_no", unique = true),
        @Index(name = "idx_order_clinic", columnList = "clinic_id"),
        @Index(name = "idx_order_status", columnList = "status")
})
public class DentureOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    /** 下单医生。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Account doctor;

    /** 患者匿名编号（不存患者真实身份信息）。 */
    @Column(nullable = false, length = 50)
    private String patientCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProsthesisType prosthesisType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImpressionMethod impressionMethod;

    /** 咬合关系描述，如 正常覆合覆盖 / 深覆合 / 反合。 */
    @Column(length = 200)
    private String biteRelation;

    /** 色号，如 VITA A2。 */
    @Column(length = 50)
    private String shade;

    /** 医生备注。 */
    @Column(length = 1000)
    private String doctorNotes;

    /** 预计试戴日期。 */
    private LocalDate expectedTryInDate;

    /** 材料要求（文本描述）。 */
    @Column(length = 500)
    private String materialRequirement;

    /** 订单难度 1-5，影响结算系数。 */
    @Column(nullable = false)
    private Integer difficulty = 1;

    /** 患者加急标记。 */
    @Column(nullable = false)
    private boolean rush = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** 全部试戴完成时间，月结按该时间归属账期。 */
    private LocalDateTime completedAt;
}
