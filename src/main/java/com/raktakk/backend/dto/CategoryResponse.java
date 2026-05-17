package com.raktakk.backend.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String icon,
        Integer displayOrder
) {
}
