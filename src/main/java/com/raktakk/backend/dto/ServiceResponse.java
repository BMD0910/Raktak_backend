package com.raktakk.backend.dto;

public record ServiceResponse(
        Long id,
        String title,
        String description,
        Double price,
        Long vendorId,
        String vendorName,
        Boolean vendorVerified,
        Boolean active
) {
}
