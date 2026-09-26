package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 医生在线批注（针对某个牙位的设计）。 */
@Getter
@Setter
@Entity
@Table(name = "design_annotation", indexes = @Index(name = "idx_ann_item", columnList = "tooth_item_id"))
public class DesignAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Account author;

    @Column(nullable = false, length = 1000)
    private String content;

    /** 是否已被技工所处理。 */
    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
