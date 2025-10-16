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

    if (store != null) {
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

      List<Visit> allStoreVisits = visits.findByStoreOrderByScheduledDateDesc(store);
      List<Visit> storeVisits = new ArrayList<>(allStoreVisits);

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

      Map<LocalDate, Long> visitsPerDay = storeVisits.stream()
          .filter(v -> v.getScheduledDate() != null)
          .collect(Collectors.groupingBy(Visit::getScheduledDate, TreeMap::new, Collectors.counting()));

      DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
      List<String> dailyChartLabels = visitsPerDay.keySet().stream()
          .map(dayFormatter::format)
          .collect(Collectors.toList());
      List<Long> dailyChartValues = new ArrayList<>(visitsPerDay.values());

      List<String> statusChartLabels = Arrays.stream(VisitStatus.values())
          .map(LojaVisitaController::labelForStatus)
          .collect(Collectors.toList());
      List<Long> statusChartValues = Arrays.stream(VisitStatus.values())
          .map(status -> statusCounts.getOrDefault(status, 0L))
          .collect(Collectors.toList());

      model.addAttribute("visits", storeVisits);
      model.addAttribute("selectedStatuses", selectedStatuses);
      model.addAttribute("selectedDay", selectedDay != null ? selectedDay.getDisplayName(TextStyle.FULL, new Locale("pt", "BR")) : "todos");
      model.addAttribute("rawDayFilter", Optional.ofNullable(dayFilter).orElse("todos"));
      model.addAttribute("range", normalizedRange);
      model.addAttribute("startDate", normalizedStart);
      model.addAttribute("endDate", normalizedEnd);
      model.addAttribute("totalVisits", totalVisits);
      model.addAttribute("completedCount", completedCount);
      model.addAttribute("pendingCount", pendingCount);
      model.addAttribute("noShowCount", noShowCount);
      model.addAttribute("reopenedCount", reopenedCount);
      model.addAttribute("completionRate", completionRate);
      model.addAttribute("todayCount", todayCount);
      model.addAttribute("yesterdayCount", yesterdayCount);
      model.addAttribute("statusChartLabels", statusChartLabels);
      model.addAttribute("statusChartValues", statusChartValues);
      model.addAttribute("dailyChartLabels", dailyChartLabels);
      model.addAttribute("dailyChartValues", dailyChartValues);
      model.addAttribute("hasOverduePending", hasOverduePending);
      model.addAttribute("overduePendingCount", overduePendingCount);
      model.addAttribute("today", today);
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
      model.addAttribute("completionRate", 0);
      model.addAttribute("todayCount", 0);
      model.addAttribute("yesterdayCount", 0);
      model.addAttribute("statusChartLabels", Arrays.stream(VisitStatus.values())
          .map(LojaVisitaController::labelForStatus)
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
    Visit visit = visits.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
      // Admins/Supers podem atualizar qualquer visita, mas se estiverem associados a uma loja diferente,
      // retornamos 404 para evitar alterações acidentais fora do contexto esperado na tela da loja.
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    visit.setStatus(status);

    boolean commentProvided = request.getParameterMap().containsKey("comment");
    if (commentProvided) {
      visit.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
    }

    boolean ratingProvided = request.getParameterMap().containsKey("rating");
    if (ratingProvided) {
      Integer rating = null;
      if (StringUtils.hasText(ratingInput)) {
        try {
          rating = Integer.parseInt(ratingInput.trim());
        } catch (NumberFormatException ex) {
          redirectAttributes.addFlashAttribute("errorMessage", "Informe uma nota numérica entre 1 e 5.");
          return resolveRedirect(target, redirect);
        }
        if (rating < 1 || rating > 5) {
          redirectAttributes.addFlashAttribute("errorMessage", "A nota deve estar entre 1 e 5.");
          return resolveRedirect(target, redirect);
        }
      }
      visit.setRating(rating);
    }

    visits.save(visit);
    redirectAttributes.addFlashAttribute("successMessage", "Visita atualizada com sucesso.");
    return resolveRedirect(target, redirect);
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
  }

  private static DayOfWeek parseDayFilter(String input) {
    if (input == null || input.isBlank() || "todos".equalsIgnoreCase(input)) {
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
}
