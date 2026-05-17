package com.raktakk.backend.dto;

public record SubcategoryResponse(
        Long id,
        Long categoryId,
        String name,
        String slug
) {
}
