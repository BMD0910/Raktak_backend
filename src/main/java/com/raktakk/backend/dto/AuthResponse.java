package com.raktakk.backend.dto;

public record AuthResponse(
        String token,
        UserMeResponse user,
        AuthProfileResponse profile
) {
}
