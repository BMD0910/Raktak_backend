package com.raktakk.backend.repository;

import com.raktakk.backend.entity.ServiceOffer;
import com.raktakk.backend.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {
    List<ServiceOffer> findByActiveTrueOrderByIdDesc();
    List<ServiceOffer> findByVendorProfileUserIdAndActiveTrueOrderByIdDesc(Long vendorId);
    List<ServiceOffer> findByVendorProfileIdAndActiveTrueOrderByIdDesc(Long profileId);
    List<ServiceOffer> findByVendorProfileUserIdOrderByIdDesc(Long vendorId);
    List<ServiceOffer> findByStatusOrderByIdDesc(ServiceStatus status);
    long countByStatus(ServiceStatus status);
    long countByVendorProfileIdAndActiveTrue(Long profileId);
    long countByVendorProfileIdAndFeaturedTrueAndActiveTrue(Long profileId);
}
