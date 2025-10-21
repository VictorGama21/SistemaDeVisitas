package com.inter.SistemaDeVisitas.service;


import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitModality;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@Service
public class VisitAnalyticsService {

  private final VisitRepository visitRepository;

  public VisitAnalyticsService(VisitRepository visitRepository) {
    this.visitRepository = visitRepository;
  }

  public List<Visit> loadVisits(Store store, LocalDate start, LocalDate end) {
    LocalDate effectiveStart = start;
    LocalDate effectiveEnd = end;

    if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
      LocalDate swap = effectiveStart;
      effectiveStart = effectiveEnd;
      effectiveEnd = swap;
    }

    List<Visit> loaded;
    if (store == null && effectiveStart == null && effectiveEnd == null) {
      loaded = visitRepository.findTop10ByOrderByScheduledDateDesc();
    } else {
      loaded = visitRepository.findByStoreAndDateRange(store, effectiveStart, effectiveEnd);
    }

    if (loaded.isEmpty()) {
      return List.of();
    }

    List<Visit> filteredByDate = new ArrayList<>(loaded.size());
    for (Visit visit : loaded) {
      LocalDate scheduled = visit.getScheduledDate();
      if (scheduled == null) {
        filteredByDate.add(visit);
        continue;
      }
      if (effectiveStart != null && scheduled.isBefore(effectiveStart)) {
        continue;
      }
      if (effectiveEnd != null && scheduled.isAfter(effectiveEnd)) {
        continue;
      }
      filteredByDate.add(visit);
    }

    return filteredByDate;
  }

  public List<Visit> applyFilters(List<Visit> visits, VisitFilterCriteria criteria) {
    if (visits == null || visits.isEmpty()) {
      return List.of();
    }

    VisitFilterCriteria effectiveCriteria = criteria != null ? criteria : VisitFilterCriteria.builder().build();

    List<Visit> filtered = new ArrayList<>();
    for (Visit visit : visits) {
      if (!matchesStatus(effectiveCriteria.getStatuses(), visit.getStatus())) {
        continue;
      }
      if (!matchesModality(effectiveCriteria.getModalities(), visit.getModality())) {
        continue;
      }
      if (!matchesBuyer(effectiveCriteria.getBuyerId(), visit)) {
        continue;
      }
      if (!matchesSupplier(effectiveCriteria.getSupplierId(), visit)) {
        continue;
      }
      if (!matchesSegment(effectiveCriteria.getSegmentId(), visit)) {
        continue;
      }
      if (!matchesDay(effectiveCriteria.getDayOfWeek(), visit.getScheduledDate())) {
        continue;
      }
      filtered.add(visit);
    }

    filtered.sort(Comparator
        .comparing(Visit::getScheduledDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
        .thenComparing(Visit::getId, Comparator.nullsLast(Comparator.reverseOrder())));
    return filtered;
  }

  public EnumMap<VisitStatus, Long> summarizeByStatus(List<Visit> visits) {
    EnumMap<VisitStatus, Long> summary = new EnumMap<>(VisitStatus.class);
    for (VisitStatus status : VisitStatus.values()) {
      summary.put(status, 0L);
    }
    if (visits == null) {
      return summary;
    }
    for (Visit visit : visits) {
      VisitStatus status = visit.getStatus();
      if (status != null) {
        summary.merge(status, 1L, Long::sum);
      }
    }
    return summary;
  }

  public NavigableMap<LocalDate, Long> summarizeDaily(List<Visit> visits) {
    NavigableMap<LocalDate, Long> summary = new TreeMap<>();
    if (visits == null) {
      return summary;
    }
    for (Visit visit : visits) {
      LocalDate date = visit.getScheduledDate();
      if (date != null) {
        summary.merge(date, 1L, Long::sum);
      }
    }
    return summary;
  }

  private boolean matchesStatus(Set<VisitStatus> statuses, VisitStatus visitStatus) {
    return statuses == null || statuses.isEmpty() || (visitStatus != null && statuses.contains(visitStatus));
  }

  private boolean matchesModality(Set<VisitModality> modalities, VisitModality modality) {
    return modalities == null || modalities.isEmpty() || (modality != null && modalities.contains(modality));
  }

  private boolean matchesBuyer(Long buyerId, Visit visit) {
    if (buyerId == null) {
      return true;
    }
    return visit.getBuyer() != null && Objects.equals(visit.getBuyer().getId(), buyerId);
  }

  private boolean matchesSupplier(Long supplierId, Visit visit) {
    if (supplierId == null) {
      return true;
    }
    return visit.getSupplier() != null && Objects.equals(visit.getSupplier().getId(), supplierId);
  }

  private boolean matchesSegment(Long segmentId, Visit visit) {
    if (segmentId == null) {
      return true;
    }
    return visit.getSegment() != null && Objects.equals(visit.getSegment().getId(), segmentId);
  }

  private boolean matchesDay(DayOfWeek dayOfWeek, LocalDate scheduledDate) {
    if (dayOfWeek == null || scheduledDate == null) {
      return true;
    }
    return scheduledDate.getDayOfWeek() == dayOfWeek;
  }
}
