package com.inter.SistemaDeVisitas.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inter.SistemaDeVisitas.entity.RegistrationToken;

public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findByToken(String token);
}
