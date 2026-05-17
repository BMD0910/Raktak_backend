package com.raktakk.backend.dto;

public record DashboardSummaryResponse(
        Long totalUsers,
        Long totalVendors,
        Long totalCategories,
        Long totalSubcategories,
        String currentRole
) {
}
