package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Buyer;
import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.Segment;
import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.Supplier;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.entity.VisitModality;
import com.inter.SistemaDeVisitas.entity.VisitStatus;
import com.inter.SistemaDeVisitas.repo.BuyerRepository;
import com.inter.SistemaDeVisitas.repo.SegmentRepository;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.SupplierRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import com.inter.SistemaDeVisitas.service.VisitAnalyticsService;
import com.inter.SistemaDeVisitas.service.VisitFilterCriteria;
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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class HomeController {

  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final VisitRepository visitRepository;
  private final BuyerRepository buyerRepository;
  private final SupplierRepository supplierRepository;
  private final SegmentRepository segmentRepository;
  private final VisitAnalyticsService visitAnalyticsService;

  public HomeController(UserRepository userRepository,
                        StoreRepository storeRepository,
                        VisitRepository visitRepository,
                        BuyerRepository buyerRepository,
                        SupplierRepository supplierRepository,
                        SegmentRepository segmentRepository,
                        VisitAnalyticsService visitAnalyticsService) {
    this.userRepository = userRepository;
    this.storeRepository = storeRepository;
    this.visitRepository = visitRepository;
    this.buyerRepository = buyerRepository;
    this.supplierRepository = supplierRepository;
    this.segmentRepository = segmentRepository;
    this.visitAnalyticsService = visitAnalyticsService;
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
                     @RequestParam(name = "visitStatus", required = false) String visitStatus,
                     @RequestParam(name = "modalidade", required = false) String modality,
                     @RequestParam(name = "buyerId", required = false) Long buyerId,
                     @RequestParam(name = "supplierId", required = false) Long supplierId,
                     @RequestParam(name = "segmentId", required = false) Long segmentId,
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

      VisitFilterCriteria criteria = buildCriteria(dayFilter, visitStatus, modality, buyerId, supplierId, segmentId);
      List<Visit> adminVisits = visitAnalyticsService.loadVisits(selectedStore, defaultStart, defaultEnd);
      List<Visit> filteredAdminVisits = visitAnalyticsService.applyFilters(adminVisits, criteria);

      EnumMap<VisitStatus, Long> statusSummary = visitAnalyticsService.summarizeByStatus(filteredAdminVisits);
      NavigableMap<LocalDate, Long> dailySummary = visitAnalyticsService.summarizeDaily(filteredAdminVisits);

      List<String> adminStatusLabels = Arrays.stream(VisitStatus.values())
          .map(VisitStatus::getSummaryLabel)
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
      model.addAttribute("showAdminShortcuts", true);
      model.addAttribute("dayFilter", selectedDay != null ? selectedDay.getDisplayName(TextStyle.FULL, new Locale("pt", "BR")) : "Todas");
      model.addAttribute("rawDayFilter", Optional.ofNullable(dayFilter).orElse("todas"));
      model.addAttribute("availableDays", buildDayFilterOptions());
      model.addAttribute("statusOptions", VisitStatus.values());
      model.addAttribute("adminSelectedStore", selectedStore);
      model.addAttribute("adminStartDate", defaultStart);
      model.addAttribute("adminEndDate", defaultEnd);
      model.addAttribute("adminSelectedStatus", visitStatus);
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
      model.addAttribute("adminCancelledCount", statusSummary.getOrDefault(VisitStatus.CANCELLED, 0L));
      model.addAttribute("adminSelectedVisitDay", Optional.ofNullable(dayFilter).orElse("todas"));
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
        List<Visit> todayVisits = visitRepository.findByStoreAndScheduledDate(store, today);
        long overduePendingCount = visitRepository.countByStoreAndStatusBefore(store, VisitStatus.PENDING, today);
        model.addAttribute("activeStore", store);
        model.addAttribute("storeVisits", weeklyVisits);
        model.addAttribute("todayVisits", todayVisits);
        model.addAttribute("today", today);
        model.addAttribute("hasOverduePending", overduePendingCount > 0);
        model.addAttribute("overduePendingCount", overduePendingCount);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("weekOptions", IntStream.rangeClosed(-4, 4).boxed().collect(Collectors.toList()));
        model.addAttribute("weekBase", weekBase);
      } else {
        visitasHoje = 0;
        clientesAtivos = 0;
        model.addAttribute("needsStoreAssociation", true);
        model.addAttribute("todayVisits", Collections.emptyList());
        model.addAttribute("today", today);
        model.addAttribute("hasOverduePending", false);
        model.addAttribute("overduePendingCount", 0L);
      }
      model.addAttribute("availableStores", storeRepository.findByActiveTrueOrderByNameAsc());
      model.addAttribute("showAdminShortcuts", false);
    } else {
      visitasHoje = visitRepository.countByScheduledDateBetween(today, today);
      clientesAtivos = 0;
      model.addAttribute("showAdminShortcuts", false);
    }

    model.addAttribute("visitasHoje", visitasHoje);
    model.addAttribute("clientesAtivos", clientesAtivos);
    model.addAttribute("currentUser", currentUser);
    model.addAttribute("isAdmin", isAdmin);
    model.addAttribute("isStoreUser", isStoreUser);
    return "home";
  }

  private VisitFilterCriteria buildCriteria(String dayFilter,
                                            String visitStatus,
                                            String modality,
                                            Long buyerId,
                                            Long supplierId,
                                            Long segmentId) {
    VisitFilterCriteria.Builder builder = VisitFilterCriteria.builder();
    DayOfWeek dayOfWeek = parseDayFilter(dayFilter);
    if (dayOfWeek != null) {
      builder.dayOfWeek(dayOfWeek);
    }
    builder.addStatus(parseVisitStatus(visitStatus));
    builder.addModality(parseVisitModality(modality));
    builder.buyerId(buyerId);
    builder.supplierId(supplierId);
    builder.segmentId(segmentId);
    return builder.build();
  }

  private VisitStatus parseVisitStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return VisitStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private VisitModality parseVisitModality(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return VisitModality.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
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
