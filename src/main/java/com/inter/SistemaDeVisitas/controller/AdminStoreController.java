package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.Store;
import com.inter.SistemaDeVisitas.repo.StoreRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/stores")
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
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
            model.addAttribute("formStore", store);
            return "admin/stores";
        }
        String trimmedName = store.getName() != null ? store.getName().trim() : null;
        store.setName(trimmedName);
        if (trimmedName == null || trimmedName.isEmpty()) {
            bindingResult.rejectValue("name", "store.name.blank", "Informe o nome da loja");
            model.addAttribute("stores", stores.findAllByOrderByNameAsc());
            model.addAttribute("filter", "todos");
            model.addAttribute("formStore", store);
            return "admin/stores";
        }
        if (store.getCnpj() != null) {
            String trimmedCnpj = store.getCnpj().trim();
            store.setCnpj(trimmedCnpj.isEmpty() ? null : trimmedCnpj);
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
