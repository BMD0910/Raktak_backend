package com.raktakk.backend.dto;

public record ServiceDTO(
        Long id,
        String title,
        String description,
        Double price,
        String category,
        String imageUrl,
        Integer deliveryTime,
        Boolean featured,
        Long vendorId,
        String vendorName,
        Boolean vendorVerified,
        Boolean active,
        String status,
        String deactivationReason,
        String deactivatedAt
) {
}
