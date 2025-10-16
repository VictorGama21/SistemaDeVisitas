package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import java.time.LocalDate;
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

  private static final Locale PT_BR = new Locale("pt", "BR");
  private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

  public LojaVisitaController(UserRepository users,
                              StoreRepository stores,
                              VisitRepository visits) {
    this.users = users;
    this.stores = stores;
    this.visits = visits;
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
    model.addAttribute("user", current);
    model.addAttribute("activeStore", store);
    model.addAttribute("stores", stores.findByActiveTrueOrderByNameAsc());
    model.addAttribute("statusOptions", VisitStatus.values());
    model.addAttribute("dayOptions", buildDayFilterOptions());
    model.addAttribute("currentQuery", request.getQueryString() == null ? "" : request.getQueryString());

    StoreVisitData data = buildStoreVisitData(store, statusFilters, startDate, endDate, dayFilter, range);

    model.addAttribute("visits", data.visits);
    model.addAttribute("selectedStatuses", data.selectedStatuses);
    model.addAttribute("selectedDay", data.selectedDayLabel);
    model.addAttribute("rawDayFilter", Optional.ofNullable(dayFilter).orElse("todos"));
    model.addAttribute("range", data.normalizedRange);
    model.addAttribute("startDate", data.normalizedStart);
    model.addAttribute("endDate", data.normalizedEnd);
    model.addAttribute("totalVisits", data.totalVisits);
    model.addAttribute("completedCount", data.completedCount);
    model.addAttribute("pendingCount", data.pendingCount);
    model.addAttribute("noShowCount", data.noShowCount);
    model.addAttribute("reopenedCount", data.reopenedCount);
    model.addAttribute("completionRate", data.completionRate);
    model.addAttribute("todayCount", data.todayCount);
    model.addAttribute("yesterdayCount", data.yesterdayCount);
    model.addAttribute("statusChartLabels", data.statusChartLabels);
    model.addAttribute("statusChartValues", data.statusChartValues);
    model.addAttribute("dailyChartLabels", data.dailyChartLabels);
    model.addAttribute("dailyChartValues", data.dailyChartValues);
    model.addAttribute("hasOverduePending", data.hasOverduePending);
    model.addAttribute("overduePendingCount", data.overduePendingCount);
    model.addAttribute("today", data.today);
    return "loja/visitas";
  }

  @GetMapping(value = "/tarefas", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> listarTarefasApi(Authentication authentication,
                                            @RequestParam(name = "status", required = false) List<String> statusFilters,
                                            @RequestParam(name = "inicio", required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(name = "fim", required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                            @RequestParam(name = "dia", required = false) String dayFilter,
                                            @RequestParam(name = "range", required = false) String range) {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Store store = current.getStore();
    if (store == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Usuário não está associado a uma loja."));
    }

    StoreVisitData data = buildStoreVisitData(store, statusFilters, startDate, endDate, dayFilter, range);
    List<VisitTaskView> tasks = data.visits.stream()
        .map(this::toVisitTaskView)
        .collect(Collectors.toList());
    List<StatusSummaryView> statusSummary = Arrays.stream(VisitStatus.values())
        .map(status -> new StatusSummaryView(status.name(), labelForStatus(status),
            data.statusCounts.getOrDefault(status, 0L)))
        .collect(Collectors.toList());
    List<DailySummaryView> dailySummary = data.dailyCounts.entrySet().stream()
        .map(entry -> new DailySummaryView(entry.getKey(),
            entry.getKey() != null ? entry.getKey().format(DAY_FORMATTER) : null,
            entry.getValue()))
        .collect(Collectors.toList());

    StoreTasksResponse response = new StoreTasksResponse(
        store.getId(),
        store.getName(),
        data.hasOverduePending,
        data.overduePendingCount,
        data.totalVisits,
        data.completedCount,
        data.pendingCount,
        data.noShowCount,
        data.reopenedCount,
        data.todayCount,
        data.yesterdayCount,
        data.completionRate,
        data.today,
        data.normalizedStart,
        data.normalizedEnd,
        data.selectedDayLabel,
        data.normalizedRange,
        data.selectedStatuses.stream().map(Enum::name).collect(Collectors.toList()),
        tasks,
        statusSummary,
        dailySummary
    );
    return ResponseEntity.ok(response);
  }

  private StoreVisitData buildStoreVisitData(Store store,
                                             List<String> statusFilters,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             String dayFilter,
                                             String range) {
    LocalDate today = LocalDate.now();
    LocalDate normalizedStart = startDate;
    LocalDate normalizedEnd = endDate;
    String normalizedRange = range == null ? "" : range.trim().toLowerCase(Locale.ROOT);

    List<String> statusChartLabels = Arrays.stream(VisitStatus.values())
        .map(LojaVisitaController::labelForStatus)
        .collect(Collectors.toList());

    if (store == null) {
      List<Long> zeroStatusValues = Arrays.stream(VisitStatus.values())
          .map(status -> 0L)
          .collect(Collectors.toList());
      return new StoreVisitData(
          Collections.emptyList(),
          Collections.emptySet(),
          "todos",
          normalizedRange,
          normalizedStart,
          normalizedEnd,
          0,
          0,
          0,
          0,
          0,
          0,
          0,
          0.0,
          statusChartLabels,
          zeroStatusValues,
          Collections.emptyList(),
          Collections.emptyList(),
          false,
          0L,
          today,
          new EnumMap<>(VisitStatus.class),
          new TreeMap<>()
      );
    }

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

    List<Visit> storeVisits = new ArrayList<>(visits.findByStoreOrderByScheduledDateDesc(store));
    long overduePendingCount = visits.countByStoreAndStatusBefore(store, VisitStatus.PENDING, today);
    boolean hasOverduePending = overduePendingCount > 0;

    if (normalizedStart != null) {
      LocalDate finalStart = normalizedStart;
      storeVisits = storeVisits.stream()
          .filter(v -> v.getScheduledDate() != null && !v.getScheduledDate().isBefore(finalStart))
          .collect(Collectors.toList());
    }
    if (normalizedEnd != null) {
      LocalDate finalEnd = normalizedEnd;
      storeVisits = storeVisits.stream()
          .filter(v -> v.getScheduledDate() != null && !v.getScheduledDate().isAfter(finalEnd))
          .collect(Collectors.toList());
    }

    Set<VisitStatus> selectedStatuses = parseStatusFilters(statusFilters);
    if (!selectedStatuses.isEmpty()) {
      storeVisits = storeVisits.stream()
          .filter(v -> selectedStatuses.contains(v.getStatus()))
          .collect(Collectors.toList());
    }

    DayOfWeek selectedDay = parseDayFilter(dayFilter);
    String selectedDayLabel = selectedDay != null ? selectedDay.getDisplayName(TextStyle.FULL, PT_BR) : "todos";
    if (selectedDay != null) {
      storeVisits = storeVisits.stream()
          .filter(v -> v.getScheduledDate() != null && v.getScheduledDate().getDayOfWeek() == selectedDay)
          .collect(Collectors.toList());
    }

    storeVisits.sort(Comparator.comparing(Visit::getScheduledDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

    Map<VisitStatus, Long> statusCounts = storeVisits.stream()
        .collect(Collectors.groupingBy(Visit::getStatus, () -> new EnumMap<>(VisitStatus.class), Collectors.counting()));

    long totalVisits = storeVisits.size();
    long completedCount = statusCounts.getOrDefault(VisitStatus.COMPLETED, 0L);
    long pendingCount = statusCounts.getOrDefault(VisitStatus.PENDING, 0L);
    long noShowCount = statusCounts.getOrDefault(VisitStatus.NO_SHOW, 0L);
    long reopenedCount = statusCounts.getOrDefault(VisitStatus.REOPENED, 0L);
    long todayCount = storeVisits.stream()
        .filter(v -> v.getScheduledDate() != null && v.getScheduledDate().isEqual(today))
        .count();
    long yesterdayCount = storeVisits.stream()
        .filter(v -> v.getScheduledDate() != null && v.getScheduledDate().isEqual(today.minusDays(1)))
        .count();

    double completionRate = totalVisits > 0 ? (completedCount * 100.0) / totalVisits : 0.0;

    NavigableMap<LocalDate, Long> visitsPerDay = storeVisits.stream()
        .filter(v -> v.getScheduledDate() != null)
        .collect(Collectors.groupingBy(Visit::getScheduledDate, TreeMap::new, Collectors.counting()));

    List<String> dailyChartLabels = visitsPerDay.keySet().stream()
        .map(DAY_FORMATTER::format)
        .collect(Collectors.toList());
    List<Long> dailyChartValues = new ArrayList<>(visitsPerDay.values());

    List<Long> statusChartValues = Arrays.stream(VisitStatus.values())
        .map(status -> statusCounts.getOrDefault(status, 0L))
        .collect(Collectors.toList());

    return new StoreVisitData(
        storeVisits,
        selectedStatuses,
        selectedDayLabel,
        normalizedRange,
        normalizedStart,
        normalizedEnd,
        totalVisits,
        completedCount,
        pendingCount,
        noShowCount,
        reopenedCount,
        todayCount,
        yesterdayCount,
        completionRate,
        statusChartLabels,
        statusChartValues,
        dailyChartLabels,
        dailyChartValues,
        hasOverduePending,
        overduePendingCount,
        today,
        statusCounts,
        visitsPerDay
    );
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
    Visit visit = prepareVisitForUpdate(current, id);
    visit.setStatus(status);

    boolean commentProvided = request.getParameterMap().containsKey("comment");
    if (commentProvided) {
      visit.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
    }

    boolean ratingProvided = request.getParameterMap().containsKey("rating");
    if (ratingProvided) {
      try {
        Integer rating = parseRatingValue(ratingInput);
        visit.setRating(rating);
      } catch (IllegalArgumentException ex) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return resolveRedirect(target, redirect);
      }
    }

    visits.save(visit);
    redirectAttributes.addFlashAttribute("successMessage", "Visita atualizada com sucesso.");
    return resolveRedirect(target, redirect);
  }

  @PostMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> atualizarStatusApi(@PathVariable Long id,
                                              @RequestBody VisitStatusUpdatePayload payload,
                                              Authentication authentication) {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Visit visit = prepareVisitForUpdate(current, id);

    VisitStatus newStatus;
    try {
      newStatus = resolveStatusValue(payload.status());
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    visit.setStatus(newStatus);

    if (payload.comment() != null) {
      visit.setComment(StringUtils.hasText(payload.comment()) ? payload.comment().trim() : null);
    }

    if (payload.rating() != null) {
      try {
        Integer rating = parseRatingValue(payload.rating());
        visit.setRating(rating);
      } catch (IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
      }
    }

    visits.save(visit);
    VisitTaskView view = toVisitTaskView(visit);
    return ResponseEntity.ok(new VisitStatusUpdateResponse("Visita atualizada com sucesso.", view));
  }
  private Visit prepareVisitForUpdate(User current, Long visitId) {
    Store store = current.getStore();
    Visit visit = visits.findDetailedById(visitId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    RoleGroup roleGroup = current.getRoleGroup();
    boolean isAdminOrSuper = roleGroup == RoleGroup.ADMIN || roleGroup == RoleGroup.SUPER;
    if (!isAdminOrSuper) {
      if (store == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      if (!visitBelongsToStore(visit, store)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    } else if (store != null && !visitBelongsToStore(visit, store)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return visit;
  }

  private Integer parseRatingValue(String ratingInput) {
    if (!StringUtils.hasText(ratingInput)) {
      return null;
    }
    String trimmed = ratingInput.trim();
    try {
      int value = Integer.parseInt(trimmed);
      if (value < 1 || value > 5) {
        throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Informe uma nota numérica entre 1 e 5.");
    }
  }

  private VisitStatus resolveStatusValue(String rawStatus) {
    if (!StringUtils.hasText(rawStatus)) {
      throw new IllegalArgumentException("Informe o status da visita.");
    }
    try {
      return VisitStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Status inválido: " + rawStatus);
    }
  }

  private VisitTaskView toVisitTaskView(Visit visit) {
    LocalDate scheduledDate = visit.getScheduledDate();
    String scheduledDateLabel = scheduledDate != null ? scheduledDate.format(FULL_DATE_FORMATTER) : null;
    String dayOfWeek = scheduledDate != null
        ? scheduledDate.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR)
        : null;
    String buyerName = visit.getBuyer() != null ? visit.getBuyer().getName() : null;
    String supplierName = visit.getSupplier() != null ? visit.getSupplier().getName() : null;
    String segmentName = visit.getSegment() != null ? visit.getSegment().getName() : null;
    String modalityLabel = visit.getModality() != null ? visit.getModality().getLabel() : null;
    return new VisitTaskView(
        visit.getId(),
        scheduledDate,
        scheduledDateLabel,
        dayOfWeek,
        visit.getStatus().name(),
        labelForStatus(visit.getStatus()),
        buyerName,
        supplierName,
        segmentName,
        modalityLabel,
        visit.getCommercialInfo(),
        visit.getComment(),
        visit.getRating()
    );
  }

  private boolean visitBelongsToStore(Visit visit, Store store) {
    if (visit.getStores() == null || store == null) {
      return false;
    }
    return visit.getStores().stream()
        .map(Store::getId)
        .filter(Objects::nonNull)
        .anyMatch(id -> id.equals(store.getId()));
  }
  private static Set<VisitStatus> parseStatusFilters(List<String> rawStatuses) {
    if (rawStatuses == null || rawStatuses.isEmpty()) {
      return Collections.emptySet();
    }
    EnumSet<VisitStatus> statuses = EnumSet.noneOf(VisitStatus.class);
    for (String raw : rawStatuses) {
      if (!StringUtils.hasText(raw)) {
        continue;
      }
      try {
        statuses.add(VisitStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
        // ignora filtros inválidos
      }
    }
    return statuses;
public class LojaVisitaController {
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

  private static List<String> buildDayFilterOptions() {
    return List.of("todos", "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo");
  }

  private static String buildRedirectSuffix(String redirect) {
    if (!StringUtils.hasText(redirect)) {
      return "";
    }
    return "?" + redirect;
  }

  private static String resolveRedirect(String target, String redirect) {
    if (StringUtils.hasText(target) && "home".equalsIgnoreCase(target.trim())) {
      return "redirect:/home";
    }
    return "redirect:/loja/visitas" + buildRedirectSuffix(redirect);
  }
  private static String labelForStatus(VisitStatus status) {
    return switch (status) {
      case COMPLETED -> "Concluída";
      case PENDING -> "Pendente";
      case NO_SHOW -> "Não realizada";
      case REOPENED -> "Reaberta";
    };
  }

  private static final class StoreVisitData {
    final List<Visit> visits;
    final Set<VisitStatus> selectedStatuses;
    final String selectedDayLabel;
    final String normalizedRange;
    final LocalDate normalizedStart;
    final LocalDate normalizedEnd;
    final long totalVisits;
    final long completedCount;
    final long pendingCount;
    final long noShowCount;
    final long reopenedCount;
    final long todayCount;
    final long yesterdayCount;
    final double completionRate;
    final List<String> statusChartLabels;
    final List<Long> statusChartValues;
    final List<String> dailyChartLabels;
    final List<Long> dailyChartValues;
    final boolean hasOverduePending;
    final long overduePendingCount;
    final LocalDate today;
    final Map<VisitStatus, Long> statusCounts;
    final NavigableMap<LocalDate, Long> dailyCounts;

    StoreVisitData(List<Visit> visits,
                   Set<VisitStatus> selectedStatuses,
                   String selectedDayLabel,
                   String normalizedRange,
                   LocalDate normalizedStart,
                   LocalDate normalizedEnd,
                   long totalVisits,
                   long completedCount,
                   long pendingCount,
                   long noShowCount,
                   long reopenedCount,
                   long todayCount,
                   long yesterdayCount,
                   double completionRate,
                   List<String> statusChartLabels,
                   List<Long> statusChartValues,
                   List<String> dailyChartLabels,
                   List<Long> dailyChartValues,
                   boolean hasOverduePending,
                   long overduePendingCount,
                   LocalDate today,
                   Map<VisitStatus, Long> statusCounts,
                   NavigableMap<LocalDate, Long> dailyCounts) {
      this.visits = visits;
      this.selectedStatuses = selectedStatuses;
      this.selectedDayLabel = selectedDayLabel;
      this.normalizedRange = normalizedRange;
      this.normalizedStart = normalizedStart;
      this.normalizedEnd = normalizedEnd;
      this.totalVisits = totalVisits;
      this.completedCount = completedCount;
      this.pendingCount = pendingCount;
      this.noShowCount = noShowCount;
      this.reopenedCount = reopenedCount;
      this.todayCount = todayCount;
      this.yesterdayCount = yesterdayCount;
      this.completionRate = completionRate;
      this.statusChartLabels = statusChartLabels;
      this.statusChartValues = statusChartValues;
      this.dailyChartLabels = dailyChartLabels;
      this.dailyChartValues = dailyChartValues;
      this.hasOverduePending = hasOverduePending;
      this.overduePendingCount = overduePendingCount;
      this.today = today;
      this.statusCounts = statusCounts;
      this.dailyCounts = dailyCounts;
    }
  }

  private record VisitTaskView(Long id,
                               LocalDate scheduledDate,
                               String scheduledDateLabel,
                               String dayOfWeek,
                               String status,
                               String statusLabel,
                               String buyer,
                               String supplier,
                               String segment,
                               String modality,
                               String commercialInfo,
                               String comment,
                               Integer rating) {
  }

  private record StatusSummaryView(String status, String label, long total) {
  }

  private record DailySummaryView(LocalDate date, String label, long total) {
  }

  private record StoreTasksResponse(Long storeId,
                                    String storeName,
                                    boolean hasOverduePending,
                                    long overduePendingCount,
                                    long totalVisits,
                                    long completedCount,
                                    long pendingCount,
                                    long noShowCount,
                                    long reopenedCount,
                                    long todayCount,
                                    long yesterdayCount,
                                    double completionRate,
                                    LocalDate today,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String selectedDay,
                                    String range,
                                    List<String> selectedStatuses,
                                    List<VisitTaskView> visits,
                                    List<StatusSummaryView> statusSummary,
                                    List<DailySummaryView> dailySummary) {
  }

  private record VisitStatusUpdatePayload(String status, String comment, String rating) {
  }

  private record VisitStatusUpdateResponse(String message, VisitTaskView visit) {
  }
}
