package com.raktakk.backend.dto;

import java.util.List;

public record SubscriptionPlanResponse(
        String code,
        String name,
        Long priceFcfa,
        String description,
        List<String> features,
        boolean active,
        Integer displayOrder,
        Integer maxServices,
        Integer maxFeaturedServices,
        boolean allowFeatured,
        boolean allowPremiumBadge,
        boolean requireCompleteProfile
) {
}
