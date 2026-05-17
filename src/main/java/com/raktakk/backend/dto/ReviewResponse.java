package com.raktakk.backend.dto;

public record ReviewResponse(
        Long id,
        Long vendorId,
        String client,
        Integer rating,
        String comment,
        String date
) {
}
