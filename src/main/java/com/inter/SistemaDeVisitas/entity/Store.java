package com.inter.SistemaDeVisitas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "stores")
public class Store {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Informe o nome da loja")
  @Size(max = 120, message = "O nome da loja deve ter no máximo 120 caracteres")
  @Column(nullable = false, length = 120)
  private String name;

  @Size(max = 32, message = "O CNPJ deve ter no máximo 32 caracteres")
  @Column(length = 32)
  private String cnpj;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  // getters/setters
  public Long getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCnpj() { return cnpj; }
  public void setCnpj(String cnpj) { this.cnpj = cnpj; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
