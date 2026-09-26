package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 材料批次与库存。 */
@Getter
@Setter
@Entity
@Table(name = "material_batch", indexes = @Index(name = "idx_batch_material", columnList = "material_id"))
public class MaterialBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(nullable = false, length = 50)
    private String batchNo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalQty = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingQty = BigDecimal.ZERO;

    private LocalDate expiryDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
