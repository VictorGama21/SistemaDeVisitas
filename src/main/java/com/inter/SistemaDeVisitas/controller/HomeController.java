package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                     @RequestParam(name = "status", required = false) String storeStatusFilter) {
    if (authentication == null) {
      return "redirect:/login";
    }

    User currentUser = userRepository.findByEmail(authentication.getName())
        .orElseThrow();

    boolean isAdmin = currentUser.getRoleGroup() == RoleGroup.ADMIN || currentUser.getRoleGroup() == RoleGroup.SUPER;
    boolean isStoreUser = currentUser.getRoleGroup() == RoleGroup.LOJA;

    ZoneId zone = ZoneId.systemDefault();
    Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
    Instant endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();

    long visitasHoje;
    long clientesAtivos;

    if (isAdmin) {
      visitasHoje = visitRepository.countByScheduledAtBetween(startOfDay, endOfDay);
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

      model.addAttribute("storeFilter", filter);
      model.addAttribute("stores", stores);
      model.addAttribute("totalStoresAtivas", storeRepository.countByActiveTrue());
      model.addAttribute("showAdminShortcuts", true);
    } else if (isStoreUser) {
      Store store = currentUser.getStore();
      if (store != null) {
        visitasHoje = visitRepository.countByStoreAndScheduledAtBetween(store, startOfDay, endOfDay);
        clientesAtivos = userRepository.countByStoreAndEnabledTrue(store);
        model.addAttribute("activeStore", store);
        model.addAttribute("storeVisits", visitRepository.findByStoreOrderByScheduledAtAsc(store)
            .stream()
            .limit(5)
            .collect(Collectors.toList()));
      } else {
        visitasHoje = 0;
        clientesAtivos = 0;
        model.addAttribute("needsStoreAssociation", true);
      }
      model.addAttribute("availableStores", storeRepository.findByActiveTrueOrderByNameAsc());
      model.addAttribute("showAdminShortcuts", false);
    } else {
      visitasHoje = visitRepository.countByScheduledAtBetween(startOfDay, endOfDay);
      clientesAtivos = userRepository.countByRoleGroupAndEnabledTrue(RoleGroup.LOJA);
      model.addAttribute("showAdminShortcuts", false);
    }

    model.addAttribute("visitasHoje", visitasHoje);
    model.addAttribute("clientesAtivos", clientesAtivos);
    model.addAttribute("isAdmin", isAdmin);
    model.addAttribute("isStoreUser", isStoreUser);

    return "home";
  }
}
