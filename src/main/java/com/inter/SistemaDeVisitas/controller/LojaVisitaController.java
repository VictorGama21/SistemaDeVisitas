package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

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
  public String listar(Model model, Authentication authentication) {
    User current = users.findByEmail(authentication.getName()).orElseThrow();
    Store store = current.getStore();
    model.addAttribute("user", current);
    model.addAttribute("activeStore", store);
    model.addAttribute("stores", stores.findByActiveTrueOrderByNameAsc());
    if (store != null) {
      model.addAttribute("visits", visits.findByStoreOrderByScheduledAtAsc(store));
    } else {
      model.addAttribute("visits", Collections.emptyList());
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
}
