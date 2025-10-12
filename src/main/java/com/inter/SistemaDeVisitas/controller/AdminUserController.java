package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository users;
    public AdminUserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", users.findAll());
        model.addAttribute("roles", RoleGroup.values());
        return "admin/users";
    }

    // Ex.: POST /admin/users/42/role?role=ADMIN
    @PostMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER')")
    public String updateRole(@PathVariable Long id, @RequestParam RoleGroup role) {
        var u = users.findById(id).orElseThrow();
        u.setRoleGroup(role);
        users.save(u);
        // redireciona para uma lista (implemente a view se quiser)
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id) {
        var user = users.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        users.save(user);
        return "redirect:/admin/users";
    }
}
