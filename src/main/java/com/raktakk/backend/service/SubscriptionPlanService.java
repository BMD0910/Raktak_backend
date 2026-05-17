package com.raktakk.backend.service;

import com.raktakk.backend.dto.SubscriptionPlanResponse;
import com.raktakk.backend.dto.SubscriptionPlanUpdateRequest;
import com.raktakk.backend.entity.SubscriptionPlan;
import com.raktakk.backend.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPublicPlans() {
        return subscriptionPlanRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAdminPlans() {
        return subscriptionPlanRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<SubscriptionPlanResponse> updatePlans(List<SubscriptionPlanUpdateRequest> requests) {
        Map<String, SubscriptionPlan> existingByCode = new HashMap<>();
        for (SubscriptionPlan existing : subscriptionPlanRepository.findAll()) {
            existingByCode.put(normalizeCode(existing.getCode()), existing);
        }

        List<SubscriptionPlan> saved = requests.stream()
                .map(request -> {
                    String key = normalizeCode(request.code());
                    SubscriptionPlan plan = existingByCode.getOrDefault(key, SubscriptionPlan.builder().build());
                    plan.setCode(key);
                    plan.setName(request.name().trim());
                    plan.setPriceFcfa(request.priceFcfa());
                    plan.setDescription(request.description().trim());
                    plan.setFeaturesText(joinFeatures(request.features()));
                    plan.setMaxServices(request.maxServices());
                    plan.setMaxFeaturedServices(request.maxFeaturedServices());
                    plan.setAllowFeatured(Boolean.TRUE.equals(request.allowFeatured()));
                    plan.setAllowPremiumBadge(Boolean.TRUE.equals(request.allowPremiumBadge()));
                    plan.setRequireCompleteProfile(Boolean.TRUE.equals(request.requireCompleteProfile()));
                    plan.setActive(Boolean.TRUE.equals(request.active()));
                    plan.setDisplayOrder(request.displayOrder());
                    return plan;
                })
                .collect(Collectors.toList());

        subscriptionPlanRepository.saveAll(saved);
        return getAdminPlans();
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getCode(),
                plan.getName(),
                plan.getPriceFcfa(),
                plan.getDescription(),
                splitFeatures(plan.getFeaturesText()),
                plan.isActive(),
                plan.getDisplayOrder(),
                plan.getMaxServices(),
                plan.getMaxFeaturedServices(),
                plan.isAllowFeatured(),
                plan.isAllowPremiumBadge(),
                plan.isRequireCompleteProfile()
        );
    }

    private String normalizeCode(String code) {
        return String.valueOf(code == null ? "" : code)
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');
    }

    private String joinFeatures(List<String> features) {
        return features.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private List<String> splitFeatures(String featuresText) {
        return String.valueOf(featuresText == null ? "" : featuresText)
                .lines()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
