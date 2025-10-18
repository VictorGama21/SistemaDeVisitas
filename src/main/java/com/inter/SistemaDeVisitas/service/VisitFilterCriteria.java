package com.inter.SistemaDeVisitas.service;

import com.inter.SistemaDeVisitas.entity.VisitModality;
import com.inter.SistemaDeVisitas.entity.VisitStatus;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class VisitFilterCriteria {

  private final Set<VisitStatus> statuses;
  private final Set<VisitModality> modalities;
  private final Long buyerId;
  private final Long supplierId;
  private final Long segmentId;
  private final DayOfWeek dayOfWeek;

  private VisitFilterCriteria(Builder builder) {
    this.statuses = builder.statuses.isEmpty()
        ? Collections.emptySet()
        : Collections.unmodifiableSet(EnumSet.copyOf(builder.statuses));
    this.modalities = builder.modalities.isEmpty()
        ? Collections.emptySet()
        : Collections.unmodifiableSet(EnumSet.copyOf(builder.modalities));
    this.buyerId = builder.buyerId;
    this.supplierId = builder.supplierId;
    this.segmentId = builder.segmentId;
    this.dayOfWeek = builder.dayOfWeek;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Set<VisitStatus> getStatuses() {
    return statuses;
  }

  public Set<VisitModality> getModalities() {
    return modalities;
  }

  public Long getBuyerId() {
    return buyerId;
  }

  public Long getSupplierId() {
    return supplierId;
  }

  public Long getSegmentId() {
    return segmentId;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public boolean hasStatusFilter() {
    return !statuses.isEmpty();
  }

  public boolean hasModalityFilter() {
    return !modalities.isEmpty();
  }

  public boolean hasBuyerFilter() {
    return buyerId != null;
  }

  public boolean hasSupplierFilter() {
    return supplierId != null;
  }

  public boolean hasSegmentFilter() {
    return segmentId != null;
  }

  public boolean hasDayOfWeekFilter() {
    return dayOfWeek != null;
  }

  public static final class Builder {
    private final Set<VisitStatus> statuses = EnumSet.noneOf(VisitStatus.class);
    private final Set<VisitModality> modalities = EnumSet.noneOf(VisitModality.class);
    private Long buyerId;
    private Long supplierId;
    private Long segmentId;
    private DayOfWeek dayOfWeek;

    private Builder() {
    }

    public Builder addStatus(VisitStatus status) {
      if (status != null) {
        statuses.add(status);
      }
      return this;
    }

    public Builder addModality(VisitModality modality) {
      if (modality != null) {
        modalities.add(modality);
      }
      return this;
    }

    public Builder buyerId(Long buyerId) {
      this.buyerId = buyerId;
      return this;
    }

    public Builder supplierId(Long supplierId) {
      this.supplierId = supplierId;
      return this;
    }

    public Builder segmentId(Long segmentId) {
      this.segmentId = segmentId;
      return this;
    }

    public Builder dayOfWeek(DayOfWeek dayOfWeek) {
      this.dayOfWeek = dayOfWeek;
      return this;
    }

    public VisitFilterCriteria build() {
      return new VisitFilterCriteria(this);
    }
  }
}
