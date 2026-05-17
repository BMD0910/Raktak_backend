package com.raktakk.backend.repository;

import com.raktakk.backend.entity.RefreshToken;
import com.raktakk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
    long deleteByExpiresAtBefore(Instant instant);
}
