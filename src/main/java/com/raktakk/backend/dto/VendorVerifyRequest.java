package com.raktakk.backend.dto;

import jakarta.validation.constraints.NotNull;

public record VendorVerifyRequest(
        @NotNull Long userId
) {
}
