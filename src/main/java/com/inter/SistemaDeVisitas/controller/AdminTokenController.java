package com.inter.SistemaDeVisitas.controller;

import com.inter.SistemaDeVisitas.entity.RegistrationToken;
import com.inter.SistemaDeVisitas.entity.RoleGroup;
import com.inter.SistemaDeVisitas.repo.RegistrationTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Controller
@RequestMapping("/admin/tokens")
public class AdminTokenController {

  private final RegistrationTokenRepository tokens;
  private final SecureRandom random = new SecureRandom();

  public AdminTokenController(RegistrationTokenRepository tokens) {
    this.tokens = tokens;
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("tokens", tokens.findAll());
    model.addAttribute("roles", RoleGroup.values());
    return "admin/tokens"; // templates/admin/tokens.html
  }

  @PostMapping("/generate")
  public String generate(@RequestParam("role") RoleGroup role,
                         @RequestParam(value="days", defaultValue="7") int days) {
    String token = generateToken(32);
    RegistrationToken t = new RegistrationToken();
    t.setToken(token);
    t.setRoleGroupAllowed(role);
    t.setExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS));
    tokens.save(t);
    return "redirect:/admin/tokens?created";
  }

  @PostMapping("/{id}/revoke")
  public String revoke(@PathVariable Long id) {
    tokens.findById(id).ifPresent(t -> { t.setUsed(true); tokens.save(t); });
    return "redirect:/admin/tokens?revoked";
  }

  private String generateToken(int bytes) {
    byte[] buf = new byte[bytes];
    random.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }
}
