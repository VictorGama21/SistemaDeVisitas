package com.inter.SistemaDeVisitas.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        // opcional: pode redirecionar para /login
        return "login";
        // ou use: return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        // renderiza templates/login.html
        return "login";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {
        // Valores padrão até que a camada de serviço seja implementada
        model.addAttribute("visitasHoje", 0);
        model.addAttribute("clientesAtivos", 0);

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER"));

        model.addAttribute("showAdminShortcuts", isAdmin);

        return "home";
    }
}
