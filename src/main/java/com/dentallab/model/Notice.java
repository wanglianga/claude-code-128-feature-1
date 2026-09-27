package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 各端同步通知：诊所端重取模通知、患者端预约调整提醒、医生端新日期同步。 */
@Getter
@Setter
@Entity
@Table(name = "notice", indexes = {
        @Index(name = "idx_notice_order", columnList = "order_id"),
        @Index(name = "idx_notice_audience", columnList = "audience")
})
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    /** 关联的取模退回单。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impression_return_id")
    private ImpressionReturn impressionReturn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticeType type;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
