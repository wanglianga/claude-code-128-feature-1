package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 配送单：签收结果影响诊所试戴安排与结算扣罚。 */
@Getter
@Setter
@Entity
@Table(name = "shipment", indexes = @Index(name = "idx_ship_order", columnList = "order_id"))
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private Account courier;

    @Column(nullable = false, unique = true, length = 40)
    private String trackingNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.SHIPPED;

    @Column(nullable = false)
    private LocalDateTime shippedAt = LocalDateTime.now();

    private LocalDateTime estimatedArrivalAt;

    private LocalDateTime deliveredAt;

    /** 签收人姓名。 */
    @Column(length = 50)
    private String signedByName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SignResult signResult;

    @Column(length = 500)
    private String signNote;

    @Column(length = 500)
    private String delayReason;
}
