package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Validated
public class RegisterController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register"; // templates/register.html
    }

    @PostMapping("/register")
    public String doRegister(
            @NotBlank String fullName,
            @Email String email,
            @NotBlank String password,
            Model model
    ) {
        // se já existe, mostra erro simples na mesma página (opcional)
        if (users.findByEmail(email).isPresent()) {
            model.addAttribute("error", "E-mail já cadastrado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRoleGroup(RoleGroup.LOJA); // padrão
        u.setEnabled(true);

        users.save(u);

        // redireciona para login com flag de sucesso
        return "redirect:/login?registered";
    }
}
