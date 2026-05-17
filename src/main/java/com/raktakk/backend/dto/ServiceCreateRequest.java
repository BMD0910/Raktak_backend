package com.raktakk.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServiceCreateRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) Double price,
        @NotBlank @Size(max = 120) String category,
        @Size(max = 512) String imageUrl,
        @NotNull @Positive Integer deliveryTime,
        Boolean featured
) {
}
