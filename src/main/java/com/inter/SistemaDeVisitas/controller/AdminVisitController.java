package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.entity.Visit;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.VisitRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/admin/visitas")
public class AdminVisitController {

    private final VisitRepository visits;
    private final StoreRepository stores;
    private final UserRepository users;

    public AdminVisitController(VisitRepository visits,
                                StoreRepository stores,
                                UserRepository users) {
        this.visits = visits;
        this.stores = stores;
        this.users = users;
    }

    @GetMapping
    public String list(@RequestParam(name = "storeId", required = false) Long storeId,
                       Model model) {
        List<Store> storeList = stores.findAllByOrderByNameAsc();
        List<Visit> visitList;
        Store selectedStore = null;

        if (storeId != null) {
            selectedStore = stores.findById(storeId).orElse(null);
        }

        if (selectedStore != null) {
            visitList = visits.findByStoreOrderByScheduledAtDesc(selectedStore);
        } else {
            visitList = visits.findTop10ByOrderByScheduledAtDesc();
        }

        model.addAttribute("stores", storeList);
        model.addAttribute("visits", visitList);
        model.addAttribute("selectedStore", selectedStore);
        return "admin/visitas";
    }

    @PostMapping
    public String schedule(@RequestParam Long storeId,
                           @RequestParam("scheduledAt")
                           @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime scheduledAt,
                           @RequestParam(required = false) String comment,
                           Authentication authentication) {
        Store store = stores.findById(storeId).orElseThrow();
        User creator = users.findByEmail(authentication.getName()).orElseThrow();

        Visit visit = new Visit();
        visit.setStore(store);
        visit.setScheduledAt(scheduledAt.atZone(ZoneId.systemDefault()).toInstant());
        visit.setComment(comment);
        visit.setCreatedBy(creator);

        visits.save(visit);

        return "redirect:/admin/visitas";
    }
}
