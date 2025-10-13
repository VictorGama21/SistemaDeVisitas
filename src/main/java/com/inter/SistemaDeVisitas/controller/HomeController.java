package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
                     @RequestParam(name = "semana", required = false) Integer weekOffsetParam) {
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
      visitasHoje = visitRepository.countByScheduledDateBetween(today, today);
      clientesAtivos = userRepository.countByRoleGroupAndEnabledTrue(RoleGroup.LOJA);
      model.addAttribute("showAdminShortcuts", false);
    }

    model.addAttribute("visitasHoje", visitasHoje);
    model.addAttribute("clientesAtivos", clientesAtivos);
    model.addAttribute("isAdmin", isAdmin);
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
}
