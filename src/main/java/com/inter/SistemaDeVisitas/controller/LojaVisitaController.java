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
public class LojaVisitaController {
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
        redirectAttributes.addFlashAttribute("errorMessage", "Informe uma nota numérica entre 1 e 5.");
        return buildRedirectUrl(redirect, target);
      }
    }

    visit.setStatus(status);
    visit.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
    visit.setRating(rating);
    visit.setLastStatusUpdatedBy(current);
    visit.setLastStatusUpdatedAt(Instant.now());

    visits.save(visit);

    redirectAttributes.addFlashAttribute("successMessage", "Visita atualizada com sucesso.");
    return buildRedirectUrl(redirect, target);
  }

  private String buildRedirectUrl(String redirect, String target) {
    StringBuilder url = new StringBuilder("redirect:/loja/visitas");
    if (StringUtils.hasText(redirect)) {
      url.append('?').append(redirect);
    }
    if (StringUtils.hasText(target)) {
      url.append('#').append(target);
    }
    return url.toString();
  }

  private Set<VisitStatus> parseStatusFilters(List<String> statusFilters) {
    if (statusFilters == null || statusFilters.isEmpty()) {
      return EnumSet.noneOf(VisitStatus.class);
    }

    EnumSet<VisitStatus> statuses = EnumSet.noneOf(VisitStatus.class);
    for (String filter : statusFilters) {
      if (!StringUtils.hasText(filter)) {
        continue;
      }
      try {
        statuses.add(VisitStatus.valueOf(filter.trim().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
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
