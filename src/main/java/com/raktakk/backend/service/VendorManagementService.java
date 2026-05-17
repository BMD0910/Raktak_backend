package com.raktakk.backend.service;

import com.raktakk.backend.dto.BecomeVendorRequest;
import com.raktakk.backend.dto.ServiceDTO;
import com.raktakk.backend.dto.ServiceUpdateRequest;
import com.raktakk.backend.dto.VendorSetupProfileRequest;
import com.raktakk.backend.dto.VendorStatusResponse;
import com.raktakk.backend.dto.VendorSubscribeRequest;
import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.ServiceOffer;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.entity.SubscriptionTransaction;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.exception.ResourceNotFoundException;
import com.raktakk.backend.repository.SubscriptionPlanRepository;
import com.raktakk.backend.repository.ProfileRepository;
import com.raktakk.backend.repository.ServiceOfferRepository;
import com.raktakk.backend.repository.UserRepository;
import com.raktakk.backend.repository.SubscriptionTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorManagementService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final UnitechPayClient unitechPayClient;

    @Value("${unitech.callbacks.base-url:http://localhost:8090}")
    private String callbacksBase;

    public VendorStatusResponse becomeVendor(String email, BecomeVendorRequest request) {
        throw new BadRequestException("Paiement d'abonnement requis avant de devenir prestataire. Utilisez /api/vendor/initiate-subscription.");
    }

    @Transactional
    public Map<String, Object> initiateSubscriptionPayment(String email, VendorSubscribeRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var plan = subscriptionPlanRepository.findByCodeIgnoreCase(request.planCode())
                .filter(p -> p.isActive())
                .orElseThrow(() -> new BadRequestException("Plan d'abonnement invalide"));

        Long amount = plan.getPriceFcfa();
        if (amount == null || amount <= 0) {
            // Les plans gratuits n'ont pas besoin de paiement : activation immédiate.
            VendorStatusResponse status = subscribeVendor(email, new VendorSubscribeRequest(plan.getCode()), null);
            Map<String, Object> out = new java.util.HashMap<>();
            out.put("free", true);
            out.put("transaction_id", null);
            out.put("payment_method", null);
            out.put("payment_url", null);
            out.put("subscription_active", true);
            out.put("subscription_plan_code", status.subscriptionPlanCode());
            out.put("subscription_plan_name", status.subscriptionPlanName());
            out.put("subscription_plan_price_fcfa", status.subscriptionPlanPriceFcfa());
            out.put("profile_completed", status.profileCompleted());
            return out;
        }

        String paymentMethod = request.paymentMethod() != null ? request.paymentMethod().toUpperCase() : "WAVE";

        // Valider la méthode de paiement
        if (!paymentMethod.matches("WAVE|ORANGE_QR|ORANGE_MAXIT|ORANGE_OM")) {
            throw new BadRequestException("Méthode de paiement invalide: " + paymentMethod);
        }

        // create local transaction
        SubscriptionTransaction tx = SubscriptionTransaction.builder()
                .user(user)
                .planCode(plan.getCode())
            .amountFcfa(amount)
                .reference("txn_local_" + System.currentTimeMillis())
                .status("PENDING")
                .paymentMethod(paymentMethod)
                .build();
        tx = subscriptionTransactionRepository.save(tx);

        String callbackBase = callbacksBase;
        String callbackSuccess = callbackBase + "/api/public/unitech/payment-success?ref=" + tx.getReference();
        String callbackCancel = callbackBase + "/api/public/unitech/payment-cancel?ref=" + tx.getReference();

        UnitechPayClient.PaymentResponse resp;
        try {
            resp = createPaymentByMethod(amount, paymentMethod, "Abonnement prestataire " + plan.getName(), callbackSuccess, callbackCancel);
        } catch (Exception e) {
            throw new BadRequestException("Impossible de créer le paiement: " + e.getMessage());
        }

        if (resp == null || !resp.success()) {
            String msg = resp == null ? "Erreur inconnue" : resp.message();
            throw new BadRequestException("Impossible de créer le paiement: " + msg);
        }

        Map<String,Object> data = resp.data();
        // update local reference with remote reference if provided
        if (data != null && data.get("reference") != null) {
            tx.setReference(String.valueOf(data.get("reference")));
            subscriptionTransactionRepository.save(tx);
        }

        java.util.Map<String,Object> out = new java.util.HashMap<>();
        out.put("payment_url", data != null ? data.get("payment_url") : null);
        out.put("transaction_id", tx.getId());
        out.put("payment_method", paymentMethod);
        // include provider-specific data to allow frontend to render QR or instructions
        if (data != null) {
            Object qr = null;
            if (data.get("qr_base64") != null) qr = data.get("qr_base64");
            else if (data.get("qr_code") != null) qr = data.get("qr_code");
            else if (data.get("qr") != null) qr = data.get("qr");
            if (qr != null) out.put("qr_base64", qr);
            out.put("provider_data", data);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTransactionForUser(String email, Long transactionId) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SubscriptionTransaction tx = subscriptionTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction introuvable"));

        if (!tx.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Transaction non autorisée");
        }

        return Map.of(
                "id", tx.getId(),
                "reference", tx.getReference(),
                "status", tx.getStatus(),
                "payment_method", tx.getPaymentMethod(),
                "amount_fcfa", tx.getAmountFcfa(),
                "created_at", tx.getCreatedAt().toString(),
                "updated_at", tx.getUpdatedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTransactionsForUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return subscriptionTransactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(tx -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", tx.getId());
                    m.put("reference", tx.getReference());
                    m.put("status", tx.getStatus());
                    m.put("payment_method", tx.getPaymentMethod());
                    m.put("plan_code", tx.getPlanCode());
                    m.put("amount_fcfa", tx.getAmountFcfa());
                    m.put("created_at", tx.getCreatedAt().toString());
                    m.put("updated_at", tx.getUpdatedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private UnitechPayClient.PaymentResponse createPaymentByMethod(Long amount, String method, String description, String callbackSuccess, String callbackCancel) {
        return switch (method) {
            case "WAVE" -> unitechPayClient.createWavePayment(amount, null, description, callbackSuccess, callbackCancel);
            case "ORANGE_QR" -> unitechPayClient.createOrangeQRPayment(amount, null, description, callbackSuccess, callbackCancel);
            case "ORANGE_MAXIT" -> unitechPayClient.createOrangeMaxItPayment(amount, null, description, callbackSuccess, callbackCancel);
            case "ORANGE_OM" -> unitechPayClient.createOrangeOMPayment(amount, null, description, callbackSuccess, callbackCancel);
            default -> throw new BadRequestException("Méthode de paiement non supportée: " + method);
        };
    }

    @Transactional
    public void activateSubscriptionFromTransaction(Long transactionId) {
        SubscriptionTransaction tx = subscriptionTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction introuvable"));

        if (!"PAID".equals(tx.getStatus())) return;

        Profile profile = profileRepository.findByUserId(tx.getUser().getId())
                .orElseGet(() -> profileRepository.save(Profile.builder()
                        .user(tx.getUser())
                        .isVendor(true)
                        .vendorVerified(false)
                        .subscriptionActive(false)
                        .profileCompleted(false)
                        .rating(0.0)
                        .totalReviews(0)
                        .build()));

        profile.setVendor(true);
        profile.setVendorVerified(false);
        profile.setSubscriptionActive(true);
        profile.setSubscriptionPlanCode(tx.getPlanCode());
        profileRepository.save(profile);
    }

    @Transactional
    public VendorStatusResponse subscribeVendor(String email, VendorSubscribeRequest request, BecomeVendorRequest legacyPayload) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request == null || request.planCode() == null || request.planCode().isBlank()) {
            throw new BadRequestException("Plan d'abonnement requis");
        }

        subscriptionPlanRepository.findByCodeIgnoreCase(request.planCode())
            .filter(plan -> plan.isActive())
            .orElseThrow(() -> new BadRequestException("Plan d'abonnement invalide"));

        if (user.getRole().name().equals("ADMIN")) {
            throw new BadRequestException("Un compte admin ne peut pas devenir prestataire");
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> profileRepository.save(Profile.builder()
                        .user(user)
                        .isVendor(false)
                        .vendorVerified(false)
                        .subscriptionActive(false)
                        .profileCompleted(false)
                        .rating(0.0)
                        .totalReviews(0)
                        .build()));

        profile.setVendor(true);
        profile.setVendorVerified(false);
        profile.setSubscriptionActive(true);
        profile.setProfileCompleted(false);
        profile.setSubscriptionPlanCode(request.planCode().trim().toUpperCase());

        if (legacyPayload != null) {
            if (legacyPayload.bio() != null) profile.setDescription(legacyPayload.bio());
            if (legacyPayload.phone() != null) profile.setPhone(legacyPayload.phone());
            if (legacyPayload.avatar() != null) profile.setAvatar(legacyPayload.avatar());
        }

        profile = profileRepository.save(profile);
        return toStatus(profile, user);
    }

    @Transactional
    public VendorStatusResponse setupProfile(String email, VendorSetupProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil prestataire introuvable"));

        if (!Boolean.TRUE.equals(profile.getSubscriptionActive())) {
            throw new BadRequestException("Abonnement prestataire requis");
        }

        profile.setVendor(true);
        profile.setProfession(request.profession().trim());
        profile.setDescription(request.description().trim());
        profile.setSkills(joinSkills(request.skills()));
        profile.setExperience(request.experience().trim());
        profile.setPhone(request.phone().trim());
        profile.setLocation(request.location().trim());
        profile.setPortfolioUrl(request.portfolioUrl() == null ? null : request.portfolioUrl().trim());
        profile.setSocialLinks(request.socialLinks() == null ? null : request.socialLinks().trim());
        if (request.avatar() != null && !request.avatar().isBlank()) {
            profile.setAvatar(request.avatar().trim());
        }

        boolean allRequiredFieldsFilled = isFilled(profile.getProfession())
                && isFilled(profile.getDescription())
                && isFilled(profile.getSkills())
                && isFilled(profile.getExperience())
                && isFilled(profile.getPhone())
                && isFilled(profile.getLocation());

        profile.setProfileCompleted(allRequiredFieldsFilled);

        return toStatus(profileRepository.save(profile), user);
    }

    @Transactional(readOnly = true)
    public VendorStatusResponse currentStatus(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder()
                        .user(user)
                        .isVendor(false)
                        .vendorVerified(false)
                        .subscriptionActive(false)
                        .profileCompleted(false)
                        .subscriptionPlanCode(null)
                        .profession(null)
                        .skills(null)
                        .experience(null)
                        .description(null)
                        .phone(null)
                        .location(null)
                        .portfolioUrl(null)
                        .socialLinks(null)
                        .avatar(null)
                        .rating(0.0)
                        .totalReviews(0)
                        .build());

        return toStatus(profile, user);
    }

    @Transactional
    public VendorStatusResponse verifyVendor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable"));

        if (!profile.isVendor()) {
            throw new BadRequestException("Cet utilisateur n'est pas prestataire");
        }

        profile.setVendorVerified(true);
        return toStatus(profileRepository.save(profile), user);
    }

    @Transactional(readOnly = true)
    public List<ServiceDTO> myServices(String email) {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BadRequestException("Profil prestataire introuvable"));

            if (!profile.isVendor()) {
                throw new BadRequestException("Cet utilisateur n'est pas prestataire");
            }

            return serviceOfferRepository.findByVendorProfileUserIdOrderByIdDesc(user.getId())
                    .stream()
                        .map(service -> toServiceDto(service, user, profile))
                    .toList();
        }

        @Transactional(readOnly = true)
        public ServiceDTO getService(String email, Long serviceId) {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BadRequestException("Profil prestataire introuvable"));

            ServiceOffer service = serviceOfferRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service introuvable"));

            if (!service.getVendorProfile().getId().equals(profile.getId())) {
                throw new BadRequestException("Vous ne pouvez modifier que vos propres services");
            }

            return toServiceDto(service, user, profile);
        }

        @Transactional
        public ServiceDTO updateService(String email, Long serviceId, ServiceUpdateRequest request) {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BadRequestException("Profil prestataire introuvable"));

            ServiceOffer service = serviceOfferRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service introuvable"));

            if (!service.getVendorProfile().getId().equals(profile.getId())) {
                throw new BadRequestException("Vous ne pouvez modifier que vos propres services");
            }

            service.setTitle(request.title().trim());
            service.setDescription(request.description().trim());
            service.setPrice(request.price());
            service.setCategory(request.category().trim());
            service.setImageUrl(request.imageUrl());
            service.setDeliveryTime(request.deliveryTime());
            if (request.featured() != null) {
                // Enforce subscription plan rules when enabling featured
                if (Boolean.TRUE.equals(request.featured()) && !Boolean.TRUE.equals(service.getFeatured())) {
                    if (profile.getSubscriptionPlanCode() != null) {
                        var plan = subscriptionPlanRepository.findByCodeIgnoreCase(profile.getSubscriptionPlanCode()).orElse(null);
                        if (plan != null) {
                            if (!plan.isAllowFeatured()) {
                                throw new BadRequestException("Votre abonnement ne permet pas de mettre des services en avant");
                            }
                            Integer maxFeatured = plan.getMaxFeaturedServices();
                            if (maxFeatured != null && maxFeatured > 0) {
                                long current = serviceOfferRepository.countByVendorProfileIdAndFeaturedTrueAndActiveTrue(profile.getId());
                                if (current >= maxFeatured) {
                                    throw new BadRequestException("Nombre maximal de services mis en avant atteint pour votre abonnement");
                                }
                            }
                        }
                    }
                }
                service.setFeatured(request.featured());
            }
            if (request.active() != null) {
                service.setActive(request.active());
            }

            ServiceOffer saved = serviceOfferRepository.save(service);
            return toServiceDto(saved, user, profile);
        }

    private VendorStatusResponse toStatus(Profile profile, User user) {
        var plan = profile.getSubscriptionPlanCode() == null ? null : subscriptionPlanRepository.findByCodeIgnoreCase(profile.getSubscriptionPlanCode()).orElse(null);
        return new VendorStatusResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                profile.isVendor(),
                Boolean.TRUE.equals(profile.getSubscriptionActive()),
                Boolean.TRUE.equals(profile.getProfileCompleted()),
            profile.getSubscriptionPlanCode(),
            plan != null ? plan.getName() : null,
            plan != null ? plan.getPriceFcfa() : null,
                profile.isVendorVerified(),
                profile.getProfession(),
                profile.getSkills(),
                profile.getExperience(),
                profile.getDescription(),
                profile.getPhone(),
                profile.getLocation(),
                profile.getPortfolioUrl(),
                profile.getSocialLinks(),
                profile.getAvatar(),
                profile.getRating(),
                profile.getTotalReviews()
        );
    }

    private ServiceDTO toServiceDto(ServiceOffer service, User user, Profile profile) {
        return new ServiceDTO(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getPrice(),
                service.getCategory(),
                service.getImageUrl(),
                service.getDeliveryTime(),
                service.getFeatured(),
                user.getId(),
                user.getFullName(),
                profile.isVendorVerified(),
                service.isActive(),
                service.getStatus() == null ? (service.isActive() ? "active" : "inactive") : service.getStatus().name().toLowerCase(),
                service.getDeactivationReason(),
                service.getDeactivatedAt() == null ? null : service.getDeactivatedAt().toString()
        );
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private String joinSkills(List<String> skills) {
        if (skills == null) {
            return "";
        }
        return skills.stream()
                .map(skill -> skill == null ? "" : skill.trim())
                .filter(this::isFilled)
                .distinct()
                .collect(Collectors.joining("\n"));
    }
}
