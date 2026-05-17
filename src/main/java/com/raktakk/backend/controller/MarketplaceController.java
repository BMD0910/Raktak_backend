package com.raktakk.backend.controller;

import com.raktakk.backend.dto.ServiceCreateRequest;
import com.raktakk.backend.dto.ServiceDTO;
import com.raktakk.backend.dto.VendorDTO;
import com.raktakk.backend.dto.VendorDetailDTO;
import com.raktakk.backend.service.MarketplaceService;
import com.raktakk.backend.service.PublicDirectoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @PostMapping("/api/services")
    public ServiceDTO createService(@Valid @RequestBody ServiceCreateRequest request,
                                    Authentication authentication) {
        return marketplaceService.create(authentication.getName(), request);
    }

    @GetMapping("/api/services")
    public List<ServiceDTO> listServices(@RequestParam(required = false) Long vendorId) {
        return marketplaceService.list(vendorId);
    }

    @GetMapping("/api/vendors")
    public List<VendorDTO> listVendors(@RequestParam(required = false) String q) {
        return marketplaceService.listVerifiedVendors(q);
    }

    @GetMapping("/api/vendors/{id}")
    public VendorDetailDTO vendorDetail(@PathVariable Long id) {
        return marketplaceService.vendorDetail(id);
    }
}
