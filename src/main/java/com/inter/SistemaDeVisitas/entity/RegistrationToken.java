package com.inter.SistemaDeVisitas.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registration_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class RegistrationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=128, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name="role_group_allowed", nullable=false, length=16)
    private RoleGroup roleGroupAllowed;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Column(nullable=false)
    private boolean used = false;

    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public RoleGroup getRoleGroupAllowed() { return roleGroupAllowed; }
    public void setRoleGroupAllowed(RoleGroup roleGroupAllowed) { this.roleGroupAllowed = roleGroupAllowed; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
