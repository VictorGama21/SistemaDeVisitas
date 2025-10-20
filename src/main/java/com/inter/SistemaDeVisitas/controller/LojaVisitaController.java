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
import org.springframework.core.io.ByteArrayResource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
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
      model.addAttribute("visits", data.getVisits());
      model.addAttribute("selectedStatuses", data.getSelectedStatuses());
      model.addAttribute("selectedDay", data.getSelectedDay() != null
          ? data.getSelectedDay().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
          : "todos");
      model.addAttribute("rawDayFilter", data.getRawDayFilter());
      model.addAttribute("range", data.getRange());
      model.addAttribute("startDate", data.getStartDate());
      model.addAttribute("endDate", data.getEndDate());
      model.addAttribute("totalVisits", data.getTotalVisits());
      model.addAttribute("completedCount", data.getCompletedCount());
      model.addAttribute("pendingCount", data.getPendingCount());
      model.addAttribute("noShowCount", data.getNoShowCount());
      model.addAttribute("reopenedCount", data.getReopenedCount());
      model.addAttribute("cancelledCount", data.getCancelledCount());
      model.addAttribute("completionRate", data.getCompletionRate());
      model.addAttribute("todayCount", data.getTodayCount());
      model.addAttribute("yesterdayCount", data.getYesterdayCount());
      model.addAttribute("statusChartLabels", data.getStatusChartLabels());
      model.addAttribute("statusChartValues", data.getStatusChartValues());
      model.addAttribute("dailyChartLabels", data.getDailyChartLabels());
      model.addAttribute("dailyChartValues", data.getDailyChartValues());
      model.addAttribute("hasOverduePending", data.hasOverduePending());
      model.addAttribute("overduePendingCount", data.getOverduePendingCount());
      model.addAttribute("today", data.getToday());
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
      model.addAttribute("completionRate", 0);
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
public class LojaVisitaController {
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
    List<Visit> storeVisits = new ArrayList<>(filteredVisits);

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
        rawDayFilter,
        storeVisits,
        totalVisits,
        completedCount,
        pendingCount,
        noShowCount,
        reopenedCount,
        cancelledCount,
        todayCount,
        yesterdayCount,
        completionRate,
        statusChartLabels,
        statusChartValues,
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

    VisitFilterCriteria criteria = data.getCriteria();

    byte[] file = visitExportService.export(data.getVisits(), data.getStartDate(), data.getEndDate(), store, criteria);
    String baseName = Optional.ofNullable(store.getName()).orElse("loja").replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.ROOT);
    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    String filename = "visitas_" + baseName + "_" + timestamp + ".xlsx";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .contentLength(file.length)
        .body(new ByteArrayResource(file));
  }

  private static class StoreVisitPageData {
    private final LocalDate today;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String range;
    private final Set<VisitStatus> selectedStatuses;
    private final DayOfWeek selectedDay;
    private final String rawDayFilter;
    private final List<Visit> visits;
    private final long totalVisits;
    private final long completedCount;
    private final long pendingCount;
    private final long noShowCount;
    private final long reopenedCount;
    private final long cancelledCount;
    private final long todayCount;
    private final long yesterdayCount;
    private final double completionRate;
    private final List<String> statusChartLabels;
    private final List<Long> statusChartValues;
    private final List<String> dailyChartLabels;
    private final List<Long> dailyChartValues;
    private final boolean hasOverduePending;
    private final long overduePendingCount;
    private final VisitFilterCriteria criteria;

    private StoreVisitPageData(LocalDate today,
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
      this.today = today;
      this.startDate = startDate;
      this.endDate = endDate;
      this.range = range;
      if (selectedStatuses == null || selectedStatuses.isEmpty()) {
        this.selectedStatuses = Collections.emptySet();
      } else {
        this.selectedStatuses = Collections.unmodifiableSet(EnumSet.copyOf(selectedStatuses));
      }
      this.selectedDay = selectedDay;
      this.rawDayFilter = rawDayFilter;
      this.visits = visits == null
          ? Collections.emptyList()
          : Collections.unmodifiableList(new ArrayList<>(visits));
      this.totalVisits = totalVisits;
      this.completedCount = completedCount;
      this.pendingCount = pendingCount;
      this.noShowCount = noShowCount;
      this.reopenedCount = reopenedCount;
      this.cancelledCount = cancelledCount;
      this.todayCount = todayCount;
      this.yesterdayCount = yesterdayCount;
      this.completionRate = completionRate;
      this.statusChartLabels = statusChartLabels == null
          ? Collections.emptyList()
          : Collections.unmodifiableList(new ArrayList<>(statusChartLabels));
      this.statusChartValues = statusChartValues == null
          ? Collections.emptyList()
          : Collections.unmodifiableList(new ArrayList<>(statusChartValues));
      this.dailyChartLabels = dailyChartLabels == null
          ? Collections.emptyList()
          : Collections.unmodifiableList(new ArrayList<>(dailyChartLabels));
      this.dailyChartValues = dailyChartValues == null
          ? Collections.emptyList()
          : Collections.unmodifiableList(new ArrayList<>(dailyChartValues));
      this.hasOverduePending = hasOverduePending;
      this.overduePendingCount = overduePendingCount;
      this.criteria = criteria;
    }

    public LocalDate getToday() {
      return today;
    }

    public LocalDate getStartDate() {
      return startDate;
    }

    public LocalDate getEndDate() {
      return endDate;
    }

    public String getRange() {
      return range;
    }

    public Set<VisitStatus> getSelectedStatuses() {
      return selectedStatuses;
    }

    public DayOfWeek getSelectedDay() {
      return selectedDay;
    }

    public String getRawDayFilter() {
      return rawDayFilter;
    }

    public List<Visit> getVisits() {
      return visits;
    }

    public long getTotalVisits() {
      return totalVisits;
    }

    public long getCompletedCount() {
      return completedCount;
    }

    public long getPendingCount() {
      return pendingCount;
    }

    public long getNoShowCount() {
      return noShowCount;
    }

    public long getReopenedCount() {
      return reopenedCount;
    }

    public long getCancelledCount() {
      return cancelledCount;
    }

    public long getTodayCount() {
      return todayCount;
    }

    public long getYesterdayCount() {
      return yesterdayCount;
    }

    public double getCompletionRate() {
      return completionRate;
    }

    public List<String> getStatusChartLabels() {
      return statusChartLabels;
    }

    public List<Long> getStatusChartValues() {
      return statusChartValues;
    }

    public List<String> getDailyChartLabels() {
      return dailyChartLabels;
    }

    public List<Long> getDailyChartValues() {
      return dailyChartValues;
    }

    public boolean hasOverduePending() {
      return hasOverduePending;
    }

    public long getOverduePendingCount() {
      return overduePendingCount;
    }

    public VisitFilterCriteria getCriteria() {
      return criteria;
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
public class LojaVisitaController {
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
        rawDayFilter,
        storeVisits,
        totalVisits,
        completedCount,
        pendingCount,
        noShowCount,
        reopenedCount,
        cancelledCount,
        todayCount,
        yesterdayCount,
        completionRate,
        statusChartLabels,
        statusChartValues,
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
    if (store == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    String effectiveRange = StringUtils.hasText(range) ? range : null;
    StoreVisitPageData data = prepareStoreVisitData(store, statusFilters, startDate, endDate, dayFilter, effectiveRange);

    VisitFilterCriteria.Builder builder = VisitFilterCriteria.builder();
    data.selectedStatuses().forEach(builder::addStatus);
    if (data.selectedDay() != null) {
      builder.dayOfWeek(data.selectedDay());
    }
    VisitFilterCriteria criteria = builder.build();

    byte[] file = visitExportService.export(data.visits(), data.startDate(), data.endDate(), store, criteria);
    String baseName = Optional.ofNullable(store.getName()).orElse("loja").replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.ROOT);
    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    String filename = "visitas_" + baseName + "_" + timestamp + ".xlsx";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
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
                                    long overduePendingCount) {
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
    Visit visit = visits.findDetailedById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (store == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "Nenhuma loja associada ao seu usuário.");
      return buildRedirectUrl(redirect, target);
    }

    boolean visitBelongsToStore = visit.getStores().stream()
        .anyMatch(s -> Objects.equals(s.getId(), store.getId()));
    if (!visitBelongsToStore) {
      redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão para alterar esta visita.");
      return buildRedirectUrl(redirect, target);
    }

    Integer rating = null;
    if (StringUtils.hasText(ratingInput)) {
      try {
        rating = Integer.parseInt(ratingInput.trim());
      } catch (NumberFormatException ex) {
        redirectAttributes.addFlashAttribute("errorMessage", "Informe uma nota numérica entre 1 e 5.");
        return buildRedirectUrl(redirect, target);
      }
      if (rating < 1 || rating > 5) {
        redirectAttributes.addFlashAttribute("errorMessage", "A nota deve estar entre 1 e 5.");
        return buildRedirectUrl(redirect, target);
      }
    }

    VisitStatus previousStatus = visit.getStatus();
    String previousComment = visit.getComment();
    Integer previousRating = visit.getRating();
    String normalizedComment = StringUtils.hasText(comment) ? comment.trim() : null;

    visit.setStatus(status);
    visit.setComment(normalizedComment);
    visit.setRating(rating);

    boolean statusChanged = previousStatus != status;
    boolean feedbackChanged = !Objects.equals(previousComment, normalizedComment)
        || !Objects.equals(previousRating, rating);

    if (statusChanged || feedbackChanged) {
      visit.setLastStatusUpdatedBy(current);
      visit.setLastStatusUpdatedAt(Instant.now());
    }

    visits.save(visit);

    redirectAttributes.addFlashAttribute("successMessage", "Visita atualizada com sucesso.");
    return buildRedirectUrl(redirect, target);
  }

  private String buildRedirectUrl(String redirect, String target) {
    StringBuilder url = new StringBuilder("redirect:/loja/visitas");
    if (StringUtils.hasText(redirect)) {
      url.append("?").append(redirect.trim());
    }
    if (StringUtils.hasText(target)) {
      String normalized = target.startsWith("#") ? target : "#" + target;
      url.append(normalized);
    }
    return url.toString();
  }

  private Set<VisitStatus> parseStatusFilters(List<String> statusFilters) {
    if (statusFilters == null || statusFilters.isEmpty()) {
      return Collections.emptySet();
    }
    EnumSet<VisitStatus> statuses = EnumSet.noneOf(VisitStatus.class);
    for (String value : statusFilters) {
      if (!StringUtils.hasText(value)) {
        continue;
      }
      try {
        statuses.add(VisitStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
        // Ignora valores inválidos
      }
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

