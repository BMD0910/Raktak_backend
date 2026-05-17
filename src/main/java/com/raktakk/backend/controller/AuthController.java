package com.raktakk.backend.controller;

import com.raktakk.backend.dto.AuthResponse;
import com.raktakk.backend.dto.LoginRequest;
import com.raktakk.backend.dto.RefreshRequest;
import com.raktakk.backend.dto.RegisterRequest;
import com.raktakk.backend.dto.UserMeResponse;
import com.raktakk.backend.service.AuthService;
import com.raktakk.backend.service.RateLimitService;
import com.raktakk.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RateLimitService rateLimitService;

    @org.springframework.beans.factory.annotation.Value("${app.refresh-cookie.name}")
    private String refreshCookieName;

    @org.springframework.beans.factory.annotation.Value("${app.refresh-cookie.secure}")
    private boolean refreshCookieSecure;

    @org.springframework.beans.factory.annotation.Value("${app.refresh-cookie.same-site}")
    private String refreshCookieSameSite;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpiryMs;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        rateLimitService.checkAuthLimit("register:" + extractClientIp(httpRequest));
        var bundle = authService.register(request);
        setRefreshCookie(httpResponse, bundle.refreshToken());
        return ResponseEntity.ok(bundle.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        rateLimitService.checkAuthLimit("login:" + extractClientIp(httpRequest));
        var bundle = authService.login(request);
        setRefreshCookie(httpResponse, bundle.refreshToken());
        return ResponseEntity.ok(bundle.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = readRefreshToken(httpRequest, refreshRequest);
        var bundle = authService.refresh(refreshToken);
        setRefreshCookie(httpResponse, bundle.refreshToken());
        return ResponseEntity.ok(bundle.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = readRefreshToken(httpRequest, refreshRequest);
        authService.logout(refreshToken);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        var user = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(userService.toMe(user));
    }

    private String readRefreshToken(HttpServletRequest request, RefreshRequest refreshRequest) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (refreshCookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return refreshRequest == null ? null : refreshRequest.refreshToken();
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.ofMillis(refreshExpiryMs))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
