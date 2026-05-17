package com.raktakk.backend.controller;

import com.raktakk.backend.repository.SiteSettingsRepository;
import com.raktakk.backend.service.CatalogService;
import com.raktakk.backend.service.PublicDirectoryService;
import com.raktakk.backend.service.SubscriptionPlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private PublicDirectoryService publicDirectoryService;

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @Mock
    private SiteSettingsRepository siteSettingsRepository;

    @Test
    void healthShouldReturnOk() throws Exception {
        PublicController controller = new PublicController(catalogService, publicDirectoryService, subscriptionPlanService, siteSettingsRepository);
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
