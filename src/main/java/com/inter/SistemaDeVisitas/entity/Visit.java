package com.inter.SistemaDeVisitas.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "visits")
public class Visit {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id")
  private Store store;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private VisitStatus status = VisitStatus.PENDING;

  @Column(columnDefinition = "text")
  private String comment;

  private Integer rating; // 1..5 (opcional)

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  // getters/setters
  public Long getId() { return id; }
  public Store getStore() { return store; }
  public void setStore(Store store) { this.store = store; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
  public VisitStatus getStatus() { return status; }
  public void setStatus(VisitStatus status) { this.status = status; }
  public String getComment() { return comment; }
  public void setComment(String comment) { this.comment = comment; }
  public Integer getRating() { return rating; }
  public void setRating(Integer rating) { this.rating = rating; }
  public User getCreatedBy() { return createdBy; }
  public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
