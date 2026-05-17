package com.raktakk.backend.dto;

public record AuthProfileResponse(
        Boolean isVendor,
        Boolean vendorVerified
) {
}
