package com.raktakk.backend.controller;

import com.raktakk.backend.dto.BecomeVendorRequest;
import com.raktakk.backend.dto.ServiceDTO;
import com.raktakk.backend.dto.ServiceUpdateRequest;
import com.raktakk.backend.dto.VendorStatusResponse;
import com.raktakk.backend.dto.VendorSetupProfileRequest;
import com.raktakk.backend.dto.VendorSubscribeRequest;
import com.raktakk.backend.dto.VendorVerifyRequest;
import com.raktakk.backend.service.VendorManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VendorController {

    private final VendorManagementService vendorManagementService;

    @PostMapping("/api/vendor/become")
    @PreAuthorize("hasRole('USER')")
    public VendorStatusResponse becomeVendor(@Valid @RequestBody(required = false) BecomeVendorRequest request,
                                             Authentication authentication) {
        return vendorManagementService.becomeVendor(authentication.getName(), request);
    }

    @PostMapping("/api/vendor/subscribe")
    @PreAuthorize("hasRole('USER')")
    public VendorStatusResponse subscribeVendor(@Valid @RequestBody VendorSubscribeRequest request,
                                                Authentication authentication) {
        return vendorManagementService.subscribeVendor(authentication.getName(), request, null);
    }

    @PostMapping("/api/vendor/initiate-subscription")
    @PreAuthorize("hasRole('USER')")
    public java.util.Map<String,Object> initiateSubscription(@Valid @RequestBody VendorSubscribeRequest request,
                                                             Authentication authentication) {
        return vendorManagementService.initiateSubscriptionPayment(authentication.getName(), request);
    }

    @GetMapping("/api/vendor/transaction/{id}")
    @PreAuthorize("hasRole('USER')")
    public java.util.Map<String,Object> getTransaction(@PathVariable Long id, Authentication authentication) {
        return vendorManagementService.getTransactionForUser(authentication.getName(), id);
    }

    @GetMapping("/api/vendor/transactions")
    @PreAuthorize("hasRole('USER')")
    public java.util.List<java.util.Map<String,Object>> listTransactions(Authentication authentication) {
        return vendorManagementService.listTransactionsForUser(authentication.getName());
    }

    @PutMapping("/api/vendor/subscription")
    @PreAuthorize("hasRole('USER')")
    public VendorStatusResponse updateSubscription(@Valid @RequestBody VendorSubscribeRequest request,
                                                   Authentication authentication) {
        return vendorManagementService.subscribeVendor(authentication.getName(), request, null);
    }

    @PostMapping("/api/vendor/setup-profile")
    @PreAuthorize("isAuthenticated()")
    public VendorStatusResponse setupProfile(@Valid @RequestBody VendorSetupProfileRequest request,
                                             Authentication authentication) {
        return vendorManagementService.setupProfile(authentication.getName(), request);
    }

    @PutMapping("/api/vendor/profile")
    @PreAuthorize("isAuthenticated()")
    public VendorStatusResponse updateProfile(@Valid @RequestBody VendorSetupProfileRequest request,
                                              Authentication authentication) {
        return vendorManagementService.setupProfile(authentication.getName(), request);
    }

    @GetMapping("/api/vendor/status")
    @PreAuthorize("isAuthenticated()")
    public VendorStatusResponse vendorStatus(Authentication authentication) {
        return vendorManagementService.currentStatus(authentication.getName());
    }

    @GetMapping("/api/vendor/services")
    @PreAuthorize("isAuthenticated()")
    public List<ServiceDTO> myServices(Authentication authentication) {
        return vendorManagementService.myServices(authentication.getName());
    }

    @GetMapping("/api/vendor/services/{id}")
    @PreAuthorize("isAuthenticated()")
    public ServiceDTO myService(@PathVariable Long id, Authentication authentication) {
        return vendorManagementService.getService(authentication.getName(), id);
    }

    @PutMapping("/api/vendor/services/{id}")
    @PreAuthorize("isAuthenticated()")
    public ServiceDTO updateService(@PathVariable Long id,
                                    @Valid @RequestBody ServiceUpdateRequest request,
                                    Authentication authentication) {
        return vendorManagementService.updateService(authentication.getName(), id, request);
    }

    @PostMapping("/api/vendor/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public VendorStatusResponse verifyVendor(@Valid @RequestBody VendorVerifyRequest request) {
        return vendorManagementService.verifyVendor(request.userId());
    }
}
