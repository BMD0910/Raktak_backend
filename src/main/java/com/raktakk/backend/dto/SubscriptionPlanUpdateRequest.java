package com.raktakk.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubscriptionPlanUpdateRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(0) Long priceFcfa,
        @NotBlank @Size(max = 600) String description,
        @NotEmpty List<@NotBlank @Size(max = 120) String> features,
        @NotNull Boolean active,
        @NotNull @Min(0) Integer displayOrder,
        @NotNull @Min(0) Integer maxServices,
        @NotNull @Min(0) Integer maxFeaturedServices,
        @NotNull Boolean allowFeatured,
        @NotNull Boolean allowPremiumBadge,
        @NotNull Boolean requireCompleteProfile
) {
}
