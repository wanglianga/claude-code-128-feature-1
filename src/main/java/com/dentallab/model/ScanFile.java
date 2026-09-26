package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 数字口扫文件登记。 */
@Getter
@Setter
@Entity
@Table(name = "scan_file", indexes = @Index(name = "idx_scan_order", columnList = "order_id"))
public class ScanFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private DentureOrder order;

    @Column(nullable = false, length = 200)
    private String fileName;

    /** 文件格式：STL / PLY / OBJ 等。 */
    @Column(length = 20)
    private String format;

    private Long sizeBytes;

    /** 存储位置说明（对象存储路径等）。 */
    @Column(length = 300)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Account uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
