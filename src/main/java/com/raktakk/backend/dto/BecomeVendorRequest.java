package com.raktakk.backend.dto;

import jakarta.validation.constraints.Size;

public record BecomeVendorRequest(
        @Size(min = 10, max = 1000) String bio,
        @Size(min = 8, max = 64) String phone,
        @Size(max = 512) String avatar
) {
}
