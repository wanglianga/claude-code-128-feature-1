package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 试戴记录：配送签收后自动生成安排，诊所回填试戴结果与患者体验。 */
@Getter
@Setter
@Entity
@Table(name = "try_in_record", indexes = @Index(name = "idx_tryin_order", columnList = "order_id"))
public class TryInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    /** 关联牙位；整单试戴记录为空。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    /** 计划试戴时间（受配送签收结果影响）。 */
    private LocalDateTime scheduledAt;

    private LocalDateTime actualAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TryInResult result;

    /** 患者试戴体验反馈。 */
    @Column(length = 1000)
    private String feedback;

    /** 患者体验评分 1-5。 */
    private Integer experienceScore;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
