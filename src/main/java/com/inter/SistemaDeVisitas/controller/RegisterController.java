package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.entity.User;
import com.inter.SistemaDeVisitas.entity.RegistrationToken;
import com.inter.SistemaDeVisitas.repo.UserRepository;
import com.inter.SistemaDeVisitas.repo.RegistrationTokenRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Instant;

@Controller
@Validated
public class RegisterController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationTokenRepository tokens; // <-- CAMPO AQUI

    // <-- CONSTRUTOR AQUI
    public RegisterController(UserRepository users,
                              PasswordEncoder passwordEncoder,
                              RegistrationTokenRepository tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
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
            @NotBlank String confirmPassword,
            @NotBlank String inviteToken, // campo do formulário
            Model model
    ) {
        // validações simples
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "As senhas não coincidem.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        if (users.findByEmail(email).isPresent()) {
            model.addAttribute("error", "E-mail já cadastrado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        // valida token
        var opt = tokens.findByToken(inviteToken);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Token inválido.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        RegistrationToken t = opt.get();
        if (t.isUsed() || t.getExpiresAt().isBefore(Instant.now())) {
            model.addAttribute("error", "Token expirado ou já utilizado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        // cria usuário com o role permitido pelo token
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRoleGroup(t.getRoleGroupAllowed());
        u.setEnabled(true);
        users.save(u);

        // marca token como usado
        t.setUsed(true);
        tokens.save(t);

        return "redirect:/login?registered";
    }
}
