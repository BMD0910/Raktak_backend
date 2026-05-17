package com.raktakk.backend.dto;

public record VendorStatusResponse(
        Long userId,
        String email,
        String fullName,
        Boolean isVendor,
        Boolean subscriptionActive,
        Boolean profileCompleted,
        String subscriptionPlanCode,
        String subscriptionPlanName,
        Long subscriptionPlanPriceFcfa,
        Boolean vendorVerified,
        String profession,
        String skills,
        String experience,
        String bio,
        String phone,
        String location,
        String portfolioUrl,
        String socialLinks,
        String avatar,
        Double rating,
        Integer totalReviews
) {
}
