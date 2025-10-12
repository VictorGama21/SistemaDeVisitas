package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/stores")
public class AdminStoreController {

    private final StoreRepository stores;

    public AdminStoreController(StoreRepository stores) {
        this.stores = stores;
    }

    @GetMapping
    public String list(@RequestParam(name = "status", required = false) String status,
                       Model model) {
        String filter = status == null ? "ativos" : status;
        switch (filter) {
            case "inativos" -> model.addAttribute("stores", stores.findByActiveFalseOrderByNameAsc());
            case "todos" -> model.addAttribute("stores", stores.findAllByOrderByNameAsc());
            default -> {
                filter = "ativos";
                model.addAttribute("stores", stores.findByActiveTrueOrderByNameAsc());
            }
        }
        model.addAttribute("filter", filter);
        model.addAttribute("formStore", new Store());
        return "admin/stores";
    }

    @PostMapping
    public String create(@ModelAttribute("formStore") @Valid Store store,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stores", stores.findAllByOrderByNameAsc());
            model.addAttribute("filter", "todos");
            return "admin/stores";
        }
        stores.save(store);
        return "redirect:/admin/stores";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        var store = stores.findById(id).orElseThrow();
        store.setActive(!store.isActive());
        stores.save(store);
        return "redirect:/admin/stores";
    }
}
