package com.raktakk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorSubscribeRequest(
        @NotBlank @Size(max = 64) String planCode,
        @Size(max = 32) String paymentMethod
) {
    public VendorSubscribeRequest(String planCode) {
        this(planCode, "WAVE");
    }
}

