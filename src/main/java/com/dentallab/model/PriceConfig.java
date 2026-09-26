package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 价格配置：各修复体类型基础单价与加急费率。 */
@Getter
@Setter
@Entity
@Table(name = "price_config")
public class PriceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private ProsthesisType prosthesisType;

    /** 每牙位基础加工费。 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    /** 加急费率（对基础费×难度系数的比例）。 */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal rushRate = new BigDecimal("0.30");
}
