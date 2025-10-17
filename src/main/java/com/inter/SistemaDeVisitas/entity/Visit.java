package com.inter.SistemaDeVisitas.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "visits")
public class Visit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToMany
  @JoinTable(name = "visit_stores",
      joinColumns = @JoinColumn(name = "visit_id"),
      inverseJoinColumns = @JoinColumn(name = "store_id"))
  private Set<Store> stores = new LinkedHashSet<>();

  @Column(name = "scheduled_date", nullable = false)
  private LocalDate scheduledDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private VisitStatus status = VisitStatus.PENDING;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_status_updated_by_user_id")
  private User lastStatusUpdatedBy;

  @Column(name = "last_status_updated_at")
  private Instant lastStatusUpdatedAt;
  @Enumerated(EnumType.STRING)
  @Column(name = "modality", nullable = false, length = 32)
  private VisitModality modality = VisitModality.PROMOTORIA_REPOSICAO;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id")
  private Buyer buyer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id")
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "segment_id")
  private Segment segment;

  @Column(columnDefinition = "text")
  private String comment;

  @Column(name = "commercial_info", columnDefinition = "text")
  private String commercialInfo;

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
    if (lastStatusUpdatedAt == null) {
      lastStatusUpdatedAt = now;
    }
    if (lastStatusUpdatedBy == null) {
      lastStatusUpdatedBy = createdBy;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Set<Store> getStores() {
    return stores;
  }

  public void setStores(Set<Store> stores) {
    this.stores = stores == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stores);
  }

  public LocalDate getScheduledDate() {
    return scheduledDate;
  }

  public void setScheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
  }

  public VisitStatus getStatus() {
    return status;
  }

  public void setStatus(VisitStatus status) {
    this.status = status;
  }

  public User getLastStatusUpdatedBy() {
    return lastStatusUpdatedBy;
  }

  public void setLastStatusUpdatedBy(User lastStatusUpdatedBy) {
    this.lastStatusUpdatedBy = lastStatusUpdatedBy;
  }

  public Instant getLastStatusUpdatedAt() {
    return lastStatusUpdatedAt;
  }

  public void setLastStatusUpdatedAt(Instant lastStatusUpdatedAt) {
    this.lastStatusUpdatedAt = lastStatusUpdatedAt;
  }

  public VisitModality getModality() {
    return modality;
  }

  public void setModality(VisitModality modality) {
    this.modality = modality;
  }

  public Buyer getBuyer() {
    return buyer;
  }

  public void setBuyer(Buyer buyer) {
    this.buyer = buyer;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  public Segment getSegment() {
    return segment;
  }

  public void setSegment(Segment segment) {
    this.segment = segment;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getCommercialInfo() {
    return commercialInfo;
  }

  public void setCommercialInfo(String commercialInfo) {
    this.commercialInfo = commercialInfo;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
