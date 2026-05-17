package com.raktakk.backend.service;

import com.raktakk.backend.entity.RefreshToken;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void persist(User user, String rawRefreshToken, Instant expiresAt, String family) {
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawRefreshToken))
                .tokenFamily(family == null ? UUID.randomUUID().toString() : family)
                .expiresAt(expiresAt)
                .revoked(false)
                .build());
    }

    public RefreshToken validateStoredToken(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Refresh token invalide"));

        if (token.isRevoked()) {
            throw new BadRequestException("Refresh token révoqué");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expiré");
        }
        return token;
    }

    public void revoke(RefreshToken token, String reason) {
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        token.setRevokeReason(reason);
        refreshTokenRepository.save(token);
    }

    public void revokeAllForUser(User user, String reason) {
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(token -> revoke(token, reason));
    }

    public String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
