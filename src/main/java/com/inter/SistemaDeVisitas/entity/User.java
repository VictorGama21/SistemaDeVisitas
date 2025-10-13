package com.inter.SistemaDeVisitas.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String fullName;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private RoleGroup roleGroup = RoleGroup.LOJA; // <- DEFAULT VÁLIDO

    @Column(nullable=false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
