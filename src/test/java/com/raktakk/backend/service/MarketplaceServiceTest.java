package com.raktakk.backend.service;

import com.raktakk.backend.dto.ServiceCreateRequest;
import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.ServiceOffer;
import com.raktakk.backend.entity.SubscriptionPlan;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.repository.ProfileRepository;
import com.raktakk.backend.repository.ServiceOfferRepository;
import com.raktakk.backend.repository.SubscriptionPlanRepository;
import com.raktakk.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock
    private ServiceOfferRepository serviceOfferRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    private MarketplaceService marketplaceService;

    @BeforeEach
    void setUp() {
        marketplaceService = new MarketplaceService(serviceOfferRepository, userRepository, profileRepository, subscriptionPlanRepository);
    }

    @Test
    void createShouldRejectWhenPlanRequiresCompleteProfile() {
        User user = user(10L, "vendor@raktakk.com", "Vendor One");
        Profile profile = profile(20L, user, false, "PRO");
        SubscriptionPlan plan = plan(true, 10, 3, true);

        when(userRepository.findByEmailIgnoreCase("vendor@raktakk.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("PRO")).thenReturn(Optional.of(plan));

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Service test",
                "Description test",
                10000.0,
                "Design",
                null,
                3,
                false
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> marketplaceService.create("vendor@raktakk.com", request));

        assertTrue(ex.getMessage().contains("Profil prestataire incomplet"));
        verify(serviceOfferRepository, never()).save(any(ServiceOffer.class));
    }

    @Test
    void createShouldRejectWhenMaxServicesReached() {
        User user = user(11L, "vendor2@raktakk.com", "Vendor Two");
        Profile profile = profile(21L, user, true, "PRO");
        SubscriptionPlan plan = plan(false, 1, 2, true);

        when(userRepository.findByEmailIgnoreCase("vendor2@raktakk.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("PRO")).thenReturn(Optional.of(plan));
        when(serviceOfferRepository.countByVendorProfileIdAndActiveTrue(21L)).thenReturn(1L);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Service test",
                "Description test",
                12000.0,
                "Marketing",
                null,
                5,
                false
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> marketplaceService.create("vendor2@raktakk.com", request));

        assertTrue(ex.getMessage().contains("Nombre maximal de services atteint"));
        verify(serviceOfferRepository, never()).save(any(ServiceOffer.class));
    }

    @Test
    void createShouldRejectWhenFeaturedNotAllowed() {
        User user = user(12L, "vendor3@raktakk.com", "Vendor Three");
        Profile profile = profile(22L, user, true, "BASIC");
        SubscriptionPlan plan = plan(false, 0, 0, false);

        when(userRepository.findByEmailIgnoreCase("vendor3@raktakk.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("BASIC")).thenReturn(Optional.of(plan));

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Service test",
                "Description test",
                9000.0,
                "Comptabilité",
                null,
                2,
                true
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> marketplaceService.create("vendor3@raktakk.com", request));

        assertTrue(ex.getMessage().contains("ne permet pas de mettre des services en avant"));
        verify(serviceOfferRepository, never()).save(any(ServiceOffer.class));
    }

    @Test
    void createShouldSucceedWhenWithinPlanLimits() {
        User user = user(13L, "vendor4@raktakk.com", "Vendor Four");
        Profile profile = profile(23L, user, true, "PRO");
        SubscriptionPlan plan = plan(true, 3, 1, true);

        when(userRepository.findByEmailIgnoreCase("vendor4@raktakk.com")).thenReturn(Optional.of(user));
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("PRO")).thenReturn(Optional.of(plan));
        when(serviceOfferRepository.countByVendorProfileIdAndActiveTrue(23L)).thenReturn(2L);
        when(serviceOfferRepository.countByVendorProfileIdAndFeaturedTrueAndActiveTrue(23L)).thenReturn(0L);
        when(serviceOfferRepository.save(any(ServiceOffer.class))).thenAnswer(invocation -> {
            ServiceOffer saved = invocation.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Service premium",
                "Description premium",
                25000.0,
                "Développement",
                "https://img.example/service.png",
                7,
                true
        );

        var result = marketplaceService.create("vendor4@raktakk.com", request);

        assertEquals(999L, result.id());
        assertEquals("Service premium", result.title());
        assertEquals(true, result.featured());
        assertEquals(13L, result.vendorId());

        ArgumentCaptor<ServiceOffer> captor = ArgumentCaptor.forClass(ServiceOffer.class);
        verify(serviceOfferRepository).save(captor.capture());
        assertEquals(profile, captor.getValue().getVendorProfile());
    }

    private User user(Long id, String email, String fullName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        return user;
    }

    private Profile profile(Long id, User user, boolean completed, String planCode) {
        Profile profile = new Profile();
        profile.setId(id);
        profile.setUser(user);
        profile.setVendor(true);
        profile.setSubscriptionActive(true);
        profile.setProfileCompleted(completed);
        profile.setVendorVerified(true);
        profile.setSubscriptionPlanCode(planCode);
        profile.setRating(0.0);
        profile.setTotalReviews(0);
        return profile;
    }

    private SubscriptionPlan plan(boolean requireComplete, int maxServices, int maxFeatured, boolean allowFeatured) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setRequireCompleteProfile(requireComplete);
        plan.setMaxServices(maxServices);
        plan.setMaxFeaturedServices(maxFeatured);
        plan.setAllowFeatured(allowFeatured);
        plan.setAllowPremiumBadge(false);
        plan.setActive(true);
        plan.setCode("PRO");
        plan.setName("Plan Pro");
        plan.setPriceFcfa(10000L);
        plan.setDescription("desc");
        plan.setFeaturesText("f1");
        plan.setDisplayOrder(1);
        return plan;
    }
}
