package com.raktakk.backend.controller;

import com.raktakk.backend.dto.SubscriptionPlanResponse;
import com.raktakk.backend.dto.SubscriptionPlanUpdateRequest;
import com.raktakk.backend.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subscription-plans")
@RequiredArgsConstructor
@Validated
public class AdminSubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping
    public List<SubscriptionPlanResponse> list() {
        return subscriptionPlanService.getAdminPlans();
    }

    @PutMapping
    public List<SubscriptionPlanResponse> update(@RequestBody @NotEmpty List<@Valid SubscriptionPlanUpdateRequest> plans) {
        return subscriptionPlanService.updatePlans(plans);
    }
}
