package com.raktakk.backend.service;

import com.raktakk.backend.dto.ServiceCreateRequest;
import com.raktakk.backend.dto.ServiceDTO;
import com.raktakk.backend.dto.VendorDTO;
import com.raktakk.backend.dto.VendorDetailDTO;
import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.ServiceOffer;
import com.raktakk.backend.entity.ServiceStatus;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.exception.NotVendorException;
import com.raktakk.backend.exception.ResourceNotFoundException;
import com.raktakk.backend.repository.ProfileRepository;
import com.raktakk.backend.repository.SubscriptionPlanRepository;
import com.raktakk.backend.entity.SubscriptionPlan;
import com.raktakk.backend.repository.ServiceOfferRepository;
import com.raktakk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final ServiceOfferRepository serviceOfferRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional
    public ServiceDTO create(String email, ServiceCreateRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("Profil prestataire introuvable"));

        checkVendorEligibility(profile);
        enforcePlanLimitsBeforeCreate(profile, request.featured() != null && request.featured());

        ServiceOffer created = serviceOfferRepository.save(ServiceOffer.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .price(request.price())
                .category(request.category().trim())
                .imageUrl(request.imageUrl())
                .deliveryTime(request.deliveryTime())
                .featured(request.featured() != null && request.featured())
                .vendorProfile(profile)
                .active(true)
                .status(ServiceStatus.ACTIVE)
                .build());

        return toDto(created, profile);
    }

    @Transactional(readOnly = true)
    public List<ServiceDTO> list(Long vendorId) {
        List<ServiceOffer> services = vendorId == null
                ? serviceOfferRepository.findByActiveTrueOrderByIdDesc()
                : serviceOfferRepository.findByVendorProfileUserIdAndActiveTrueOrderByIdDesc(vendorId);

        return services.stream().map(s -> toDto(s, s.getVendorProfile())).toList();
    }

    @Transactional(readOnly = true)
    public List<VendorDTO> listVerifiedVendors(String query) {
        return profileRepository.findByIsVendorTrueAndVendorVerifiedTrue()
                .stream()
                .filter(profile -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase().trim();
                    return profile.getUser().getFullName().toLowerCase().contains(q)
                            || (profile.getDescription() != null && profile.getDescription().toLowerCase().contains(q));
                })
                .map(this::toVendorDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VendorDTO> listVerifiedVendorsTransactional(String query) {
        return listVerifiedVendors(query);
    }

    @Transactional(readOnly = true)
    public VendorDetailDTO vendorDetail(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestataire introuvable"));

        if (!profile.isVendor()) {
            throw new NotVendorException("Cet utilisateur n'est pas prestataire");
        }

        List<ServiceDTO> services = serviceOfferRepository.findByVendorProfileIdAndActiveTrueOrderByIdDesc(profile.getId())
                .stream()
                .map(service -> toDto(service, profile))
                .toList();

        return new VendorDetailDTO(toVendorDto(profile), services);
    }

    public void checkVendorEligibility(Profile profile) {
        if (!profile.isVendor()) {
            throw new NotVendorException("Seuls les prestataires peuvent créer un service");
        }
        if (!Boolean.TRUE.equals(profile.getSubscriptionActive())) {
            throw new BadRequestException("Abonnement prestataire requis");
        }
        // Respecter la configuration du plan concernant le profil complet
        SubscriptionPlan plan = null;
        if (profile.getSubscriptionPlanCode() != null) {
            plan = subscriptionPlanRepository.findByCodeIgnoreCase(profile.getSubscriptionPlanCode()).orElse(null);
        }
        boolean requireComplete = true;
        if (plan != null) {
            requireComplete = plan.isRequireCompleteProfile();
        }
        if (requireComplete && !Boolean.TRUE.equals(profile.getProfileCompleted())) {
            throw new BadRequestException("Profil prestataire incomplet");
        }
    }

    private void enforcePlanLimitsBeforeCreate(Profile profile, boolean requestedFeatured) {
        if (profile.getSubscriptionPlanCode() == null) return;
        SubscriptionPlan plan = subscriptionPlanRepository.findByCodeIgnoreCase(profile.getSubscriptionPlanCode()).orElse(null);
        if (plan == null) return;

        Integer maxServices = plan.getMaxServices();
        if (maxServices != null && maxServices > 0) {
            long current = serviceOfferRepository.countByVendorProfileIdAndActiveTrue(profile.getId());
            if (current >= maxServices) {
                throw new BadRequestException("Nombre maximal de services atteint pour votre abonnement");
            }
        }

        if (requestedFeatured) {
            if (!plan.isAllowFeatured()) {
                throw new BadRequestException("Votre abonnement ne permet pas de mettre des services en avant");
            }
            Integer maxFeatured = plan.getMaxFeaturedServices();
            if (maxFeatured != null && maxFeatured > 0) {
                long currentFeatured = serviceOfferRepository.countByVendorProfileIdAndFeaturedTrueAndActiveTrue(profile.getId());
                if (currentFeatured >= maxFeatured) {
                    throw new BadRequestException("Nombre maximal de services mis en avant atteint pour votre abonnement");
                }
            }
        }
    }

    private ServiceDTO toDto(ServiceOffer service, Profile profile) {
        return new ServiceDTO(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getPrice(),
                service.getCategory(),
                service.getImageUrl(),
                service.getDeliveryTime(),
                service.getFeatured(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile != null && profile.isVendorVerified(),
                service.isActive(),
                service.getStatus() == null ? (service.isActive() ? "active" : "inactive") : service.getStatus().name().toLowerCase(),
                service.getDeactivationReason(),
                service.getDeactivatedAt() == null ? null : service.getDeactivatedAt().toString()
        );
    }

    private VendorDTO toVendorDto(Profile profile) {
        var user = profile.getUser();
        return new VendorDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getDescription(),
                profile.getPhone(),
                profile.getAvatar(),
                profile.getRating(),
                profile.getTotalReviews(),
                profile.isVendorVerified()
        );
    }
}
