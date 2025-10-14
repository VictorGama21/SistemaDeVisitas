package com.inter.SistemaDeVisitas.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity // habilita @PreAuthorize, etc. (se você usar)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Mantém CSRF habilitado (padrão). Seus forms devem enviar o token.
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // público
                .requestMatchers(
                    "/", "/login", "/register", "/error",
                    "/favicon.ico",
                    "/img/**", "/css/**", "/js/**",
                    "/actuator/health"
                ).permitAll()

                // admin + super
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER")

                // área da loja (loja vê; admin/super também podem)
                .requestMatchers("/loja/**").hasAnyRole("LOJA", "ADMIN", "SUPER")

                // home autenticado
                .requestMatchers(HttpMethod.GET, "/home").authenticated()

                // qualquer outra rota: autenticado
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")            // GET para exibir a página
                .loginProcessingUrl("/login")   // POST do formulário
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(l -> l
                .logoutUrl("/logout")               // POST recomendado
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // Se você usar H2 console em DEV, descomente abaixo:
        // http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        // http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(HttpServletRequest request,
                                               HttpServletResponse response,
                                               Authentication authentication) {
                Set<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
                if (authorities.contains("ROLE_LOJA")) {
                    return "/loja/visitas";
                }
                return super.determineTargetUrl(request, response, authentication);
            }
        };
        handler.setDefaultTargetUrl("/home");
        handler.setAlwaysUseDefaultTargetUrl(false);
        return handler;
    }
}
