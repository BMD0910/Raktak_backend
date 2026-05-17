package com.raktakk.backend.service;

import com.raktakk.backend.dto.AuthResponse;
import com.raktakk.backend.dto.AuthProfileResponse;
import com.raktakk.backend.dto.LoginRequest;
import com.raktakk.backend.dto.RegisterRequest;
import com.raktakk.backend.dto.UserMeResponse;
import com.raktakk.backend.entity.AuthProvider;
import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.RefreshToken;
import com.raktakk.backend.entity.Role;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.repository.ProfileRepository;
import com.raktakk.backend.repository.UserRepository;
import com.raktakk.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ProfileRepository profileRepository;

    public record AuthBundle(AuthResponse response, String refreshToken, String refreshFamily) {}

    public AuthBundle register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            log.warn("REGISTER_REJECTED email_already_used={}", request.email());
            throw new BadRequestException("Email already used");
        }

        User user = userRepository.save(User.builder()
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .build());

            profileRepository.save(Profile.builder()
                .user(user)
                .isVendor(false)
                .vendorVerified(false)
                .subscriptionActive(false)
                .profileCompleted(false)
                .rating(0.0)
                .totalReviews(0)
                .build());

            String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole());
            String refreshToken = jwtService.generateRefreshToken(user.getEmail());
            String family = UUID.randomUUID().toString();
            refreshTokenService.persist(user, refreshToken, Instant.now().plusMillis(getRefreshExpiryMs()), family);

            log.info("REGISTER_SUCCESS user_id={} email={}", user.getId(), user.getEmail());
            return new AuthBundle(
                toAuthResponse(user, accessToken),
                refreshToken,
                family
            );
    }

            public AuthBundle login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        String family = UUID.randomUUID().toString();
        refreshTokenService.persist(user, refreshToken, Instant.now().plusMillis(getRefreshExpiryMs()), family);

        log.info("LOGIN_SUCCESS user_id={} email={}", user.getId(), user.getEmail());
        return new AuthBundle(
            toAuthResponse(user, accessToken),
                refreshToken,
                family
        );
    }

    public AuthBundle refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadRequestException("Refresh token manquant");
        }

        String email;
        try {
            email = jwtService.extractUsernameFromRefresh(rawRefreshToken);
            if (!jwtService.isRefreshTokenValid(rawRefreshToken, email)) {
                throw new BadRequestException("Refresh token invalide");
            }
        } catch (Exception ex) {
            log.warn("REFRESH_REJECTED reason={}", ex.getMessage());
            throw new BadRequestException("Refresh token invalide");
        }

        RefreshToken storedToken = refreshTokenService.validateStoredToken(rawRefreshToken);
        User user = storedToken.getUser();

        refreshTokenService.revoke(storedToken, "ROTATED");
        String newAccessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());
        refreshTokenService.persist(user, newRefreshToken, Instant.now().plusMillis(getRefreshExpiryMs()), storedToken.getTokenFamily());

        log.info("REFRESH_SUCCESS user_id={} email={}", user.getId(), user.getEmail());
        return new AuthBundle(
            toAuthResponse(user, newAccessToken),
                newRefreshToken,
                storedToken.getTokenFamily()
        );
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.info("LOGOUT_WITHOUT_REFRESH_TOKEN");
            return;
        }
        RefreshToken stored = refreshTokenService.validateStoredToken(rawRefreshToken);
        refreshTokenService.revoke(stored, "LOGOUT");
        refreshTokenService.revokeAllForUser(stored.getUser(), "LOGOUT_FAMILY_REVOKE");
        log.info("LOGOUT_SUCCESS user_id={} email={}", stored.getUser().getId(), stored.getUser().getEmail());
    }

    @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpiryMs;

    private long getRefreshExpiryMs() {
        return refreshExpiryMs;
    }

    private AuthResponse toAuthResponse(User user, String accessToken) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> profileRepository.save(Profile.builder()
                        .user(user)
                        .isVendor(false)
                        .vendorVerified(false)
                .subscriptionActive(false)
                .profileCompleted(false)
                .rating(0.0)
                .totalReviews(0)
                        .build()));

        return new AuthResponse(
                accessToken,
                new UserMeResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.getAccountStatus() == null ? "active" : user.getAccountStatus().name().toLowerCase(),
                        user.getDeactivationReason(),
                        user.getDeactivationContact()
                ),
            new AuthProfileResponse(profile.isVendor(), profile.isVendorVerified())
        );
    }
}
