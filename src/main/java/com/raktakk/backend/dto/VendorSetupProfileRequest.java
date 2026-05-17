package com.raktakk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VendorSetupProfileRequest(
        @NotBlank @Size(min = 2, max = 120) String profession,
        @NotBlank @Size(min = 10, max = 1000) String description,
        @NotEmpty List<@NotBlank @Size(min = 2, max = 120) String> skills,
        @NotBlank @Size(min = 3, max = 2000) String experience,
        @NotBlank @Size(min = 8, max = 64) String phone,
        @NotBlank @Size(min = 2, max = 120) String location,
        @Size(max = 512) String portfolioUrl,
        @Size(max = 512) String avatar,
        @Size(max = 1000) String socialLinks
) {
}
