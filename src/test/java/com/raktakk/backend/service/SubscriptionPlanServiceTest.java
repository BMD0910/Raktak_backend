package com.raktakk.backend.service;

import com.raktakk.backend.dto.SubscriptionPlanUpdateRequest;
import com.raktakk.backend.entity.SubscriptionPlan;
import com.raktakk.backend.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    private SubscriptionPlanService subscriptionPlanService;

    @BeforeEach
    void setUp() {
        subscriptionPlanService = new SubscriptionPlanService(subscriptionPlanRepository);
    }

    @Test
    void updatePlansShouldPersistAndReturnRuleFields() {
        SubscriptionPlan existing = new SubscriptionPlan();
        existing.setCode("PRO");
        existing.setName("Ancien Pro");

        SubscriptionPlan updated = new SubscriptionPlan();
        updated.setCode("PRO_PLUS");
        updated.setName("Pro Plus");
        updated.setPriceFcfa(25000L);
        updated.setDescription("Plan avancé");
        updated.setFeaturesText("Support prioritaire\nBadge premium");
        updated.setMaxServices(40);
        updated.setMaxFeaturedServices(8);
        updated.setAllowFeatured(true);
        updated.setAllowPremiumBadge(true);
        updated.setRequireCompleteProfile(false);
        updated.setActive(true);
        updated.setDisplayOrder(2);

        when(subscriptionPlanRepository.findAll()).thenReturn(List.of(existing));
        when(subscriptionPlanRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionPlanRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(updated));

        List<SubscriptionPlanUpdateRequest> requests = List.of(new SubscriptionPlanUpdateRequest(
                "pro plus",
                "Pro Plus",
                25000L,
                "Plan avancé",
                List.of("Support prioritaire", "Badge premium"),
                true,
                2,
                40,
                8,
                true,
                true,
                false
        ));

        var response = subscriptionPlanService.updatePlans(requests);

        assertEquals(1, response.size());
        assertEquals("PRO_PLUS", response.get(0).code());
        assertEquals(40, response.get(0).maxServices());
        assertEquals(8, response.get(0).maxFeaturedServices());
        assertTrue(response.get(0).allowFeatured());
        assertTrue(response.get(0).allowPremiumBadge());

        ArgumentCaptor<List<SubscriptionPlan>> captor = ArgumentCaptor.forClass(List.class);
        verify(subscriptionPlanRepository).saveAll(captor.capture());

        SubscriptionPlan savedPlan = captor.getValue().get(0);
        assertEquals("PRO_PLUS", savedPlan.getCode());
        assertEquals("Support prioritaire\nBadge premium", savedPlan.getFeaturesText());
        assertEquals(40, savedPlan.getMaxServices());
        assertEquals(8, savedPlan.getMaxFeaturedServices());
        assertTrue(savedPlan.isAllowFeatured());
        assertTrue(savedPlan.isAllowPremiumBadge());
        assertEquals(false, savedPlan.isRequireCompleteProfile());
    }
}
