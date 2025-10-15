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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class HomeController {

  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final VisitRepository visitRepository;

  public HomeController(UserRepository userRepository,
                        StoreRepository storeRepository,
                        VisitRepository visitRepository) {
    this.userRepository = userRepository;
    this.storeRepository = storeRepository;
    this.visitRepository = visitRepository;
  }

  @GetMapping("/")
  public String root() {
    return "login";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/home")
  public String home(Model model,
                     Authentication authentication,
                     @RequestParam(name = "status", required = false) String storeStatusFilter,
                     @RequestParam(name = "dia", required = false) String dayFilter,
                     @RequestParam(name = "semana", required = false) Integer weekOffsetParam,
                     @RequestParam(name = "storeId", required = false) Long storeId,
                     @RequestParam(name = "inicio", required = false)
                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                     @RequestParam(name = "fim", required = false)
                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                     HttpServletRequest request) {
    if (authentication == null) {
      return "redirect:/login";
    }

    User currentUser = userRepository.findByEmail(authentication.getName())
        .orElseThrow();

    boolean isAdmin = currentUser.getRoleGroup() == RoleGroup.ADMIN || currentUser.getRoleGroup() == RoleGroup.SUPER;
    boolean isStoreUser = currentUser.getRoleGroup() == RoleGroup.LOJA;

    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);

    long visitasHoje;
    long clientesAtivos;

    if (isAdmin) {
      visitasHoje = visitRepository.countByScheduledDateBetween(today, today);
      clientesAtivos = userRepository.countByRoleGroupAndEnabledTrue(RoleGroup.LOJA);

      String filter = Optional.ofNullable(storeStatusFilter)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .orElse("ativos");
      List<Store> stores = switch (filter) {
        case "inativos" -> storeRepository.findByActiveFalseOrderByNameAsc();
        case "todos" -> storeRepository.findAllByOrderByNameAsc();
        default -> storeRepository.findByActiveTrueOrderByNameAsc();
      };

      Store selectedStore = null;
      if (storeId != null) {
        selectedStore = storeRepository.findById(storeId).orElse(null);
      }

      LocalDate defaultEnd = Optional.ofNullable(endDate).orElse(today);
      LocalDate defaultStart = Optional.ofNullable(startDate).orElse(defaultEnd.minusDays(29));
      if (defaultEnd.isBefore(defaultStart)) {
        LocalDate swap = defaultStart;
        defaultStart = defaultEnd;
        defaultEnd = swap;
      }

      Map<VisitStatus, Long> statusSummary = new EnumMap<>(VisitStatus.class);
      for (Object[] row : visitRepository.countByStoreAndStatusBetween(selectedStore, defaultStart, defaultEnd)) {
        VisitStatus status = (VisitStatus) row[0];
        Long total = (Long) row[1];
        statusSummary.put(status, total);
      }

      NavigableMap<LocalDate, Long> dailySummary = new TreeMap<>();
      for (Object[] row : visitRepository.countDailyByStoreBetween(selectedStore, defaultStart, defaultEnd)) {
        LocalDate date = (LocalDate) row[0];
        Long total = (Long) row[1];
        dailySummary.put(date, total);
      }

      List<String> adminStatusLabels = Arrays.stream(VisitStatus.values())
          .map(HomeController::labelForStatus)
          .toList();
      List<Long> adminStatusValues = Arrays.stream(VisitStatus.values())
          .map(status -> statusSummary.getOrDefault(status, 0L))
          .toList();
      long adminTotalVisits = adminStatusValues.stream().mapToLong(Long::longValue).sum();

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
      List<String> adminDailyLabels = dailySummary.keySet().stream()
          .map(formatter::format)
          .toList();
      List<Long> adminDailyValues = new ArrayList<>(dailySummary.values());

      model.addAttribute("storeSelection", storeRepository.findByActiveTrueOrderByNameAsc());
      DayOfWeek selectedDay = parseDayFilter(dayFilter);
      List<Visit> upcomingVisits = visitRepository.findTop20ByScheduledDateGreaterThanEqualOrderByScheduledDateAsc(today);
      if (selectedDay != null) {
        upcomingVisits = upcomingVisits.stream()
            .filter(v -> v.getScheduledDate() != null && v.getScheduledDate().getDayOfWeek() == selectedDay)
            .collect(Collectors.toList());
      }

      model.addAttribute("storeFilter", filter);
      model.addAttribute("stores", stores);
      model.addAttribute("totalStoresAtivas", storeRepository.countByActiveTrue());
      model.addAttribute("showAdminShortcuts", true);
      model.addAttribute("dayFilter", selectedDay != null ? selectedDay.getDisplayName(TextStyle.FULL, new Locale("pt", "BR")) : "Todas");
      model.addAttribute("rawDayFilter", Optional.ofNullable(dayFilter).orElse("todas"));
      model.addAttribute("availableDays", buildDayFilterOptions());
      model.addAttribute("upcomingVisits", upcomingVisits);
      model.addAttribute("adminSelectedStore", selectedStore);
      model.addAttribute("adminStartDate", defaultStart);
      model.addAttribute("adminEndDate", defaultEnd);
      model.addAttribute("adminStatusLabels", adminStatusLabels);
      model.addAttribute("adminStatusValues", adminStatusValues);
      model.addAttribute("adminDailyLabels", adminDailyLabels);
      model.addAttribute("adminDailyValues", adminDailyValues);
      model.addAttribute("adminStatusSummary", statusSummary);
      model.addAttribute("adminTotalVisits", adminTotalVisits);
      model.addAttribute("adminCompletedCount", statusSummary.getOrDefault(VisitStatus.COMPLETED, 0L));
      model.addAttribute("adminPendingCount", statusSummary.getOrDefault(VisitStatus.PENDING, 0L));
      model.addAttribute("adminNoShowCount", statusSummary.getOrDefault(VisitStatus.NO_SHOW, 0L));
      model.addAttribute("adminReopenedCount", statusSummary.getOrDefault(VisitStatus.REOPENED, 0L));
    } else if (isStoreUser) {
      Store store = currentUser.getStore();
      if (store != null) {
        int weekOffset = Optional.ofNullable(weekOffsetParam).orElse(0);
        LocalDate weekBase = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekStart = weekBase.plusWeeks(weekOffset);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Visit> weeklyVisits = visitRepository.findByStoreAndScheduledDateBetween(store, weekStart, weekEnd);
        visitasHoje = visitRepository.countByStoreAndScheduledDateBetween(store, today, today);
        clientesAtivos = userRepository.countByStoreAndEnabledTrue(store);
        model.addAttribute("activeStore", store);
        model.addAttribute("storeVisits", weeklyVisits);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("weekOptions", IntStream.rangeClosed(-4, 4).boxed().collect(Collectors.toList()));
        model.addAttribute("weekBase", weekBase);
      } else {
        visitasHoje = 0;
        clientesAtivos = 0;
        model.addAttribute("needsStoreAssociation", true);
      }
      model.addAttribute("availableStores", storeRepository.findByActiveTrueOrderByNameAsc());
      model.addAttribute("showAdminShortcuts", false);
    } else {
@@ -137,26 +200,35 @@ public class HomeController {
    model.addAttribute("isStoreUser", isStoreUser);

    return "home";
  }

  private DayOfWeek parseDayFilter(String input) {
    if (input == null || input.isBlank() || "todas".equalsIgnoreCase(input)) {
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
    return List.of("todas", "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo");
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
