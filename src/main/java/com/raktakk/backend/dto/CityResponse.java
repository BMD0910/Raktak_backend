package com.raktakk.backend.dto;

public record CityResponse(
        String name,
        String country,
        Long vendors,
        String emoji
) {
}
