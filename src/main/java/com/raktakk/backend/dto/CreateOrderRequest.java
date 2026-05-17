package com.raktakk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull(message = "serviceId est requis")
    Long serviceId,

    @NotBlank(message = "description est requise")
    String description
) {}
