package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import com.inter.SistemaDeVisitas.service.VisitAnalyticsService;
import com.inter.SistemaDeVisitas.service.VisitExportService;
import com.inter.SistemaDeVisitas.service.VisitFilterCriteria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/loja/visitas")
public class LojaVisitaController {

  private final UserRepository users;
  private final StoreRepository stores;
  private final VisitRepository visits;
  private final VisitAnalyticsService visitAnalytics;
  private final VisitExportService visitExportService;

  public LojaVisitaController(UserRepository users,
                              StoreRepository stores,
                              VisitRepository visits,
                              VisitAnalyticsService visitAnalytics,
                              VisitExportService visitExportService) {
    this.users = users;
    this.stores = stores;
    this.visits = visits;
    this.visitAnalytics = visitAnalytics;
    this.visitExportService = visitExportService;
  }

  @GetMapping
  public String listar(Model model,
                       Authentication authentication,
                       @RequestParam(name = "status", required = false) List<String> statusFilters,
                       @RequestParam(name = "inicio", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(name = "fim", required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(name = "dia", required = false) String dayFilter,
                       @RequestParam(name = "range", required = false) String range,
                       HttpServletRequest request) {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Store store = current.getStore();
    RoleGroup roleGroup = Optional.ofNullable(current.getRoleGroup()).orElse(RoleGroup.LOJA);

    model.addAttribute("user", current);
    model.addAttribute("activeStore", store);
    model.addAttribute("stores", stores.findByActiveTrueOrderByNameAsc());
    model.addAttribute("statusOptions", VisitStatus.values());
    model.addAttribute("dayOptions", buildDayFilterOptions());
    model.addAttribute("currentQuery", request.getQueryString() == null ? "" : request.getQueryString());
    model.addAttribute("roleGroup", roleGroup);

    if (store != null) {
      String effectiveRange = request.getParameterMap().containsKey("range") ? range : null;
      StoreVisitPageData data = prepareStoreVisitData(store, statusFilters, startDate, endDate, dayFilter, effectiveRange);

      model.addAttribute("visits", data.visits());
      model.addAttribute("selectedStatuses", data.selectedStatuses());
      model.addAttribute("selectedDay", data.selectedDay() != null
          ? data.selectedDay().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
          : "todos");
      model.addAttribute("rawDayFilter", data.rawDayFilter());
      model.addAttribute("range", data.range());
      model.addAttribute("startDate", data.startDate());
      model.addAttribute("endDate", data.endDate());
      model.addAttribute("totalVisits", data.totalVisits());
      model.addAttribute("completedCount", data.completedCount());
      model.addAttribute("pendingCount", data.pendingCount());
      model.addAttribute("noShowCount", data.noShowCount());
      model.addAttribute("reopenedCount", data.reopenedCount());
      model.addAttribute("cancelledCount", data.cancelledCount());
      model.addAttribute("completionRate", data.completionRate());
      model.addAttribute("todayCount", data.todayCount());
      model.addAttribute("yesterdayCount", data.yesterdayCount());
      model.addAttribute("statusChartLabels", data.statusChartLabels());
      model.addAttribute("statusChartValues", data.statusChartValues());
      model.addAttribute("dailyChartLabels", data.dailyChartLabels());
      model.addAttribute("dailyChartValues", data.dailyChartValues());
      model.addAttribute("hasOverduePending", data.hasOverduePending());
      model.addAttribute("overduePendingCount", data.overduePendingCount());
      model.addAttribute("today", data.today());
    } else {
      model.addAttribute("visits", Collections.emptyList());
      model.addAttribute("selectedStatuses", Collections.emptySet());
      model.addAttribute("selectedDay", "todos");
      model.addAttribute("rawDayFilter", "todos");
      model.addAttribute("range", "");
      model.addAttribute("startDate", null);
      model.addAttribute("endDate", null);
      model.addAttribute("totalVisits", 0);
      model.addAttribute("completedCount", 0);
      model.addAttribute("pendingCount", 0);
      model.addAttribute("noShowCount", 0);
      model.addAttribute("reopenedCount", 0);
      model.addAttribute("cancelledCount", 0);
      model.addAttribute("completionRate", 0.0);
      model.addAttribute("todayCount", 0);
      model.addAttribute("yesterdayCount", 0);
      model.addAttribute("statusChartLabels", Arrays.stream(VisitStatus.values())
          .map(VisitStatus::getLabel)
          .collect(Collectors.toList()));
      model.addAttribute("statusChartValues", Arrays.stream(VisitStatus.values())
          .map(status -> 0L)
          .collect(Collectors.toList()));
      model.addAttribute("dailyChartLabels", Collections.emptyList());
      model.addAttribute("dailyChartValues", Collections.emptyList());
      model.addAttribute("hasOverduePending", false);
      model.addAttribute("overduePendingCount", 0L);
      model.addAttribute("today", LocalDate.now());
    }

    return "loja/visitas";
  }

  private StoreVisitPageData prepareStoreVisitData(Store store,
                                                  List<String> statusFilters,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  String dayFilter,
                                                  String range) {
    LocalDate today = LocalDate.now();
    LocalDate normalizedStart = startDate;
    LocalDate normalizedEnd = endDate;
    String normalizedRange = range == null ? "" : range.trim().toLowerCase(Locale.ROOT);

    if ("hoje".equals(normalizedRange)) {
      normalizedStart = today;
      normalizedEnd = today;
    } else if ("ontem".equals(normalizedRange)) {
      normalizedStart = today.minusDays(1);
      normalizedEnd = today.minusDays(1);
    }

    if (normalizedStart != null && normalizedEnd != null && normalizedEnd.isBefore(normalizedStart)) {
      LocalDate swap = normalizedStart;
      normalizedStart = normalizedEnd;
      normalizedEnd = swap;
    }

    Set<VisitStatus> selectedStatuses = parseStatusFilters(statusFilters);
    DayOfWeek selectedDay = parseDayFilter(dayFilter);

    VisitFilterCriteria.Builder criteriaBuilder = VisitFilterCriteria.builder();
    selectedStatuses.forEach(criteriaBuilder::addStatus);
    if (selectedDay != null) {
      criteriaBuilder.dayOfWeek(selectedDay);
    }
    VisitFilterCriteria criteria = criteriaBuilder.build();

    List<Visit> loadedVisits = visitAnalytics.loadVisits(store, normalizedStart, normalizedEnd);
    List<Visit> filteredVisits = visitAnalytics.applyFilters(loadedVisits, criteria);
    List<Visit> storeVisits = List.copyOf(filteredVisits);

    EnumMap<VisitStatus, Long> statusCounts = visitAnalytics.summarizeByStatus(storeVisits);

    long totalVisits = storeVisits.size();
    long completedCount = statusCounts.getOrDefault(VisitStatus.COMPLETED, 0L);
    long pendingCount = statusCounts.getOrDefault(VisitStatus.PENDING, 0L);
    long noShowCount = statusCounts.getOrDefault(VisitStatus.NO_SHOW, 0L);
    long reopenedCount = statusCounts.getOrDefault(VisitStatus.REOPENED, 0L);
    long cancelledCount = statusCounts.getOrDefault(VisitStatus.CANCELLED, 0L);
    long todayCount = storeVisits.stream()
        .filter(v -> v.getScheduledDate() != null && v.getScheduledDate().isEqual(today))
        .count();
    long yesterdayCount = storeVisits.stream()
        .filter(v -> v.getScheduledDate() != null && v.getScheduledDate().isEqual(today.minusDays(1)))
        .count();

    double completionRate = totalVisits > 0 ? (completedCount * 100.0) / totalVisits : 0.0;

    NavigableMap<LocalDate, Long> visitsPerDay = visitAnalytics.summarizeDaily(storeVisits);
    DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
    List<String> dailyChartLabels = visitsPerDay.keySet().stream()
        .map(dayFormatter::format)
        .collect(Collectors.toList());
    List<Long> dailyChartValues = new ArrayList<>(visitsPerDay.values());

    List<String> statusChartLabels = Arrays.stream(VisitStatus.values())
        .map(VisitStatus::getLabel)
        .collect(Collectors.toList());
    List<Long> statusChartValues = Arrays.stream(VisitStatus.values())
        .map(status -> statusCounts.getOrDefault(status, 0L))
        .collect(Collectors.toList());

    long overduePendingCount = visits.countByStoreAndStatusBefore(store, VisitStatus.PENDING, today);
    boolean hasOverduePending = overduePendingCount > 0;

    String rawDayFilter = Optional.ofNullable(dayFilter).orElse("todos");

    return new StoreVisitPageData(
        today,
        normalizedStart,
        normalizedEnd,
        normalizedRange,
        selectedStatuses,
        selectedDay,
public class LojaVisitaController {
        dailyChartLabels,
        dailyChartValues,
        hasOverduePending,
        overduePendingCount,
        criteria
    );
  }

  @GetMapping("/exportar")
  public ResponseEntity<ByteArrayResource> exportar(Authentication authentication,
                                                    @RequestParam(name = "status", required = false) List<String> statusFilters,
                                                    @RequestParam(name = "inicio", required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                    @RequestParam(name = "fim", required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                    @RequestParam(name = "dia", required = false) String dayFilter,
                                                    @RequestParam(name = "range", required = false) String range) throws IOException {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Store store = current.getStore();
    if (store == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    String effectiveRange = StringUtils.hasText(range) ? range : null;
    StoreVisitPageData data = prepareStoreVisitData(store, statusFilters, startDate, endDate, dayFilter, effectiveRange);
    VisitFilterCriteria criteria = data.criteria();

    byte[] file = visitExportService.export(data.visits(), data.startDate(), data.endDate(), store, criteria);
    String baseName = Optional.ofNullable(store.getName()).orElse("loja").replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.ROOT);
    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    String filename = "visitas_" + baseName + "_" + timestamp + ".xlsx";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .contentLength(file.length)
        .body(new ByteArrayResource(file));
  }

  private record StoreVisitPageData(LocalDate today,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String range,
                                    Set<VisitStatus> selectedStatuses,
                                    DayOfWeek selectedDay,
                                    String rawDayFilter,
                                    List<Visit> visits,
                                    long totalVisits,
                                    long completedCount,
                                    long pendingCount,
                                    long noShowCount,
                                    long reopenedCount,
                                    long cancelledCount,
                                    long todayCount,
                                    long yesterdayCount,
                                    double completionRate,
                                    List<String> statusChartLabels,
                                    List<Long> statusChartValues,
                                    List<String> dailyChartLabels,
                                    List<Long> dailyChartValues,
                                    boolean hasOverduePending,
                                    long overduePendingCount,
                                    VisitFilterCriteria criteria) {
    private StoreVisitPageData {
      range = range == null ? "" : range;
      selectedStatuses = selectedStatuses == null || selectedStatuses.isEmpty()
          ? Set.of()
          : Collections.unmodifiableSet(EnumSet.copyOf(selectedStatuses));
      rawDayFilter = rawDayFilter == null ? "todos" : rawDayFilter;
      visits = visits == null ? List.of() : List.copyOf(visits);
      statusChartLabels = statusChartLabels == null ? List.of() : List.copyOf(statusChartLabels);
      statusChartValues = statusChartValues == null ? List.of() : List.copyOf(statusChartValues);
      dailyChartLabels = dailyChartLabels == null ? List.of() : List.copyOf(dailyChartLabels);
      dailyChartValues = dailyChartValues == null ? List.of() : List.copyOf(dailyChartValues);
    }
  }

  @PostMapping("/associar")
  public String associar(@RequestParam Long storeId, Authentication authentication) {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Store store = stores.findById(storeId)
        .filter(Store::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    current.setStore(store);
    users.save(current);
    return "redirect:/loja/visitas";
  }

  @PostMapping("/{id}/status")
  public String atualizarStatus(@PathVariable Long id,
                                @RequestParam VisitStatus status,
                                @RequestParam(name = "comment", required = false) String comment,
                                @RequestParam(name = "rating", required = false) String ratingInput,
                                @RequestParam(name = "redirect", required = false) String redirect,
                                @RequestParam(name = "target", required = false) String target,
                                Authentication authentication,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Store store = current.getStore();
public class LojaVisitaController {
    }
    return statuses;
  }

  private DayOfWeek parseDayFilter(String input) {
    if (input == null || input.isBlank() || "todos".equalsIgnoreCase(input) || "todas".equalsIgnoreCase(input)) {
      return null;
    }
    String normalized = input.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "segunda" -> DayOfWeek.MONDAY;
      case "terca", "terça" -> DayOfWeek.TUESDAY;
      case "quarta" -> DayOfWeek.WEDNESDAY;
      case "quinta" -> DayOfWeek.THURSDAY;
      case "sexta" -> DayOfWeek.FRIDAY;
      case "sabado", "sábado" -> DayOfWeek.SATURDAY;
      case "domingo" -> DayOfWeek.SUNDAY;
      default -> null;
    };
  }

  private List<String> buildDayFilterOptions() {
    return List.of("todos", "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo");
  }
}
