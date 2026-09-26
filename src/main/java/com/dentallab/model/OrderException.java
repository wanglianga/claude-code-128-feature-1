package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 异常事件：把诊所、技工、质检、配送、客服、财务连接到同一颗牙。 */
@Getter
@Setter
@Entity
@Table(name = "order_exception", indexes = @Index(name = "idx_exc_order", columnList = "order_id"))
public class OrderException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    /** 关联牙位；订单级异常为空。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExceptionType type;

    @Column(nullable = false, length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by")
    private Account reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private Account handledBy;

    @Column(length = 1000)
    private String resolution;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime handledAt;
}
