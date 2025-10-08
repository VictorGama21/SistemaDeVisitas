package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public RegisterController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/register")
    public String form(Model model) {
        return "register"; // templates/register.html
    }

    @PostMapping("/register")
    public String submit(@RequestParam String fullName,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String confirmPassword,
                         Model model,
                         RedirectAttributes ra) {

        // validações básicas
        if (fullName == null || fullName.isBlank()) {
            model.addAttribute("error", "Informe o nome completo.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Informe o e-mail.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Senha deve ter pelo menos 6 caracteres.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "As senhas não conferem.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (users.findByEmail(email.toLowerCase().trim()).isPresent()) {
            model.addAttribute("error", "Este e-mail já está cadastrado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        // cria usuário como LOJA por padrão
        var u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(email.toLowerCase().trim());
        u.setPassword(encoder.encode(password));
        u.setRoleGroup(RoleGroup.LOJA);
        u.setEnabled(true);
        users.save(u);

        ra.addFlashAttribute("registered", true);
        return "redirect:/login?registered";
    }
}
