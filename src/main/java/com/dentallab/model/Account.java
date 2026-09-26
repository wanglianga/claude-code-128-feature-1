package com.dentallab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 系统账号（诊所侧与技工所侧共用）。 */
@Getter
@Setter
@Entity
@Table(name = "account", indexes = @Index(name = "idx_account_username", columnList = "username", unique = true))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 演示环境使用明文密码，生产应替换为散列。 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /** 诊所账号所属诊所；技工所账号为空。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;
}
