package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 多端同步通知：患者端提醒、医生端新日期、诊所端确认等消息留痕。 */
@Getter
@Setter
@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notice_order", columnList = "order_id"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    /** 关联的取模缺陷退回单；其他来源的通知为空。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impression_return_id")
    private ImpressionReturn impressionReturn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipientType recipientType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
