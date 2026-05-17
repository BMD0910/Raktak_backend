package com.raktakk.backend.controller;

import com.raktakk.backend.dto.CategoryResponse;
import com.raktakk.backend.dto.CityResponse;
import com.raktakk.backend.dto.ReviewResponse;
import com.raktakk.backend.dto.SubscriptionPlanResponse;
import com.raktakk.backend.dto.SubcategoryResponse;
import com.raktakk.backend.dto.VendorResponse;
import com.raktakk.backend.entity.SiteSettings;
import com.raktakk.backend.service.CatalogService;
import com.raktakk.backend.service.PublicDirectoryService;
import com.raktakk.backend.service.SubscriptionPlanService;
import com.raktakk.backend.repository.SiteSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CatalogService catalogService;
    private final PublicDirectoryService publicDirectoryService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final SiteSettingsRepository siteSettingsRepository;

    @Value("${mail.admin:admin@raktakk.com}")
    private String defaultAdminEmail;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "raktakk-backend");
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.categories();
    }

    @GetMapping("/subcategories")
    public List<SubcategoryResponse> subcategories(@RequestParam Long categoryId) {
        return catalogService.subcategories(categoryId);
    }

    @GetMapping("/vendors")
    public List<VendorResponse> vendors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city
    ) {
        return publicDirectoryService.vendors(q, category, city);
    }

    @GetMapping("/vendors/{id}")
    public VendorResponse vendor(@PathVariable Long id) {
        return publicDirectoryService.vendorById(id)
                .orElseThrow(() -> new com.raktakk.backend.exception.ResourceNotFoundException("Vendor not found"));
    }

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return publicDirectoryService.cities();
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews(@RequestParam(required = false) Long vendorId) {
        return publicDirectoryService.reviews(vendorId);
    }

    @GetMapping("/subscription-plans")
    public List<SubscriptionPlanResponse> subscriptionPlans() {
        return subscriptionPlanService.getPublicPlans();
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        SiteSettings settings = getOrCreateSettings();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("siteName", settings.getSiteName());
        map.put("supportEmail", settings.getSupportEmail());
        map.put("supportPhone", settings.getSupportPhone());
        map.put("maintenanceMode", settings.isMaintenanceMode());
        map.put("maintenanceMessage", settings.getMaintenanceMessage());
        return Map.of("ok", true, "data", map);
    }

    private SiteSettings getOrCreateSettings() {
        return siteSettingsRepository.findFirstByOrderByUpdatedAtDesc().orElseGet(() -> siteSettingsRepository.save(SiteSettings.builder()
                .siteName("Raktakk")
            .supportEmail(defaultAdminEmail)
                .supportPhone("+221 77 000 00 00")
                .maintenanceMode(false)
                .maintenanceMessage("")
                .auditRetentionDays(90)
                .allowNewRegistrations(true)
                .allowNewVendorApplications(true)
                .build()));
    }
}
