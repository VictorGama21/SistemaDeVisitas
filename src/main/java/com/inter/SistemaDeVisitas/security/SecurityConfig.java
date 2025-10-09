// src/main/java/com/inter/SistemaDeVisitas/security/SecurityConfig.java
package com.inter.SistemaDeVisitas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/login", "/css/**", "/js/**", "/img/**", "/register", "/actuator/health").permitAll()
        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER")
        .anyRequest().authenticated()
      )
      .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")
        .defaultSuccessUrl("/home", true)
        .failureUrl("/login?error")
        .permitAll()
      )
      .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());
    return http.build();
  }

  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
