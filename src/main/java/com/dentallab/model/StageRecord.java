package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 工序记录：每个牙位每道工序一条，记录操作人、材料批次、照片、预计完成时间。 */
@Getter
@Setter
@Entity
@Table(name = "stage_record", indexes = {
        @Index(name = "idx_stage_item", columnList = "tooth_item_id"),
        @Index(name = "idx_stage_operator", columnList = "operator_id")
})
public class StageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tooth_item_id")
    private ToothItem toothItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StageType stage;

    /** 操作人。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private Account operator;

    /** 使用材料批次（可选）。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_batch_id")
    private MaterialBatch materialBatch;

    /** 本工序材料用量。 */
    @Column(precision = 12, scale = 2)
    private BigDecimal materialQty;

    /** 照片（多个以英文逗号分隔的地址/文件名）。 */
    @Column(length = 1000)
    private String photos;

    @Column(length = 500)
    private String note;

    /** 预计完成时间。 */
    private LocalDateTime estimatedFinishAt;

    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    /** 实际完成时间；null 表示进行中。 */
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
