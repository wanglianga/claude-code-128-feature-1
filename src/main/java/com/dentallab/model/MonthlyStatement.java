package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 诊所月结单。确认前必须复核账期内全部返工。 */
@Getter
@Setter
@Entity
@Table(name = "monthly_statement",
        uniqueConstraints = @UniqueConstraint(name = "uk_statement_clinic_month", columnNames = {"clinic_id", "year_month"}))
public class MonthlyStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    /** 账期，格式 yyyy-MM。 */
    @Column(nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false)
    private Integer orderCount = 0;

    @Column(nullable = false, precision = 14, scale = 2)
    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

    /** 生成时账期内未复核返工数（>0 时不允许确认）。 */
    @Column(nullable = false)
    private Integer unreviewedReworkCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatementStatus status = StatementStatus.GENERATED;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;
}
