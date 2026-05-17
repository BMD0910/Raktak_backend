package com.raktakk.backend.dto;

import java.util.List;

public record VendorResponse(
        Long id,
        String name,
        String category,
        String city,
        String country,
        Double rating,
        Integer reviews,
        Boolean verified,
        String badge,
        String emoji,
        String description,
        List<String> services,
        String price,
        Boolean available,
        Long views,
        Long leads
) {
}
