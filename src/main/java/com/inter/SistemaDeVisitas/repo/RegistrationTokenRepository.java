package com.inter.SistemaDeVisitas.repo;

import com.inter.SistemaDeVisitas.entity.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findByToken(String token);
    List<RegistrationToken> findAllByExpiresAtAfterOrderByExpiresAtAsc(Instant now);
}
