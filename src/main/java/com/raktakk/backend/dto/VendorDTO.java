package com.raktakk.backend.dto;

public record VendorDTO(
        Long id,
        String fullName,
        String email,
        String bio,
        String phone,
        String avatar,
        Double rating,
        Integer totalReviews,
        Boolean vendorVerified
) {
}
