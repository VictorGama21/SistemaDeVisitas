package com.inter.SistemaDeVisitas.entity;
import com.inter.SistemaDeVisitas.repo.RegistrationTokenRepository;
import com.inter.SistemaDeVisitas.entity.RegistrationToken; // (se usar diretamente)

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidade responsável pelos tokens de registro controlados pelo administrador.
 * Cada token libera o cadastro de um usuário de determinado grupo (RoleGroup)
 * e expira automaticamente após a data definida.
 */
@Entity
@Table(name = "registration_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class RegistrationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_group_allowed", nullable = false, length = 16)
    private RoleGroup roleGroupAllowed;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    // === CONSTRUTORES ===
    public RegistrationToken() {}

    public RegistrationToken(String token, RoleGroup roleGroupAllowed, Instant expiresAt) {
        this.token = token;
        this.roleGroupAllowed = roleGroupAllowed;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    // === GETTERS & SETTERS ===
    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public RoleGroup getRoleGroupAllowed() {
        return roleGroupAllowed;
    }

    public void setRoleGroupAllowed(RoleGroup roleGroupAllowed) {
        this.roleGroupAllowed = roleGroupAllowed;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    // === MÉTODOS UTILITÁRIOS ===
    @Transient
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    @Override
    public String toString() {
        return "RegistrationToken{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", roleGroupAllowed=" + roleGroupAllowed +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                '}';
    }
}
