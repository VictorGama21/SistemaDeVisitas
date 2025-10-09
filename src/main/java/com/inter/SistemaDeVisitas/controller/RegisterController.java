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
    private final RegistrationTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserRepository users,
                              RegistrationTokenRepository tokens,
                              PasswordEncoder passwordEncoder) {
        this.users = users;
        this.tokens = tokens;
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
            @NotBlank String confirmPassword,
            @NotBlank String registrationToken,
            Model model
    ) {
        // 1) valida senhas
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "As senhas não conferem.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        // 2) verifica se e-mail já existe
        if (users.findByEmail(email).isPresent()) {
            model.addAttribute("error", "E-mail já cadastrado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        // 3) valida token
        var now = Instant.now();
        RegistrationToken token = tokens.findByToken(registrationToken).orElse(null);
        if (token == null) {
            model.addAttribute("error", "Token inválido.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (token.isUsed()) {
            model.addAttribute("error", "Token já utilizado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(now)) {
            model.addAttribute("error", "Token expirado.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        // Por regra de negócio, só ADMIN/LOJA via token
        RoleGroup allowed = token.getRoleGroupAllowed();
        if (allowed == null || allowed == RoleGroup.SUPER) {
            model.addAttribute("error", "Token inválido para cadastro.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        // 4) cria usuário com role do token e marca token como usado
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRoleGroup(allowed); // ADMIN ou LOJA vindos do token
        u.setEnabled(true);

        users.save(u);

        token.setUsed(true);
        tokens.save(token);

        // 5) sucesso
        return "redirect:/login?registered";
    }
}
