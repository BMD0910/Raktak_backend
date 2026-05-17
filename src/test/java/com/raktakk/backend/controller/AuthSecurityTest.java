package com.raktakk.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raktakk.backend.dto.AuthProfileResponse;
import com.raktakk.backend.dto.AuthResponse;
import com.raktakk.backend.dto.UserMeResponse;
import com.raktakk.backend.entity.AccountStatus;
import com.raktakk.backend.entity.Role;
import com.raktakk.backend.service.AuthService;
import com.raktakk.backend.service.RateLimitService;
import com.raktakk.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AuthSecurityTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private RateLimitService rateLimitService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService, userService, rateLimitService);
        ReflectionTestUtils.setField(controller, "refreshCookieName", "raktakk_refresh_token");
        ReflectionTestUtils.setField(controller, "refreshCookieSecure", false);
        ReflectionTestUtils.setField(controller, "refreshCookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "refreshExpiryMs", 1209600000L);

        mockMvc = standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void loginShouldReturnAccessTokenAndRefreshCookie() throws Exception {
        doNothing().when(rateLimitService).checkAuthLimit(anyString());
        when(authService.login(org.mockito.ArgumentMatchers.any(com.raktakk.backend.dto.LoginRequest.class)))
                .thenReturn(new AuthService.AuthBundle(
                        new AuthResponse(
                                "access-token",
                                new UserMeResponse(1L, "admin@raktakk.com", "Super Admin", Role.ADMIN, AccountStatus.ACTIVE.getValue(), null, null),
                                new AuthProfileResponse(false, false)
                        ),
                        "refresh-token",
                        "refresh-family"
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "admin@raktakk.com",
                                "password", "Admin123*"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("raktakk_refresh_token=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void refreshShouldIssueNewAccessToken() throws Exception {
        when(authService.refresh(anyString())).thenReturn(new AuthService.AuthBundle(
                new AuthResponse(
                        "new-access-token",
                        new UserMeResponse(1L, "admin@raktakk.com", "Super Admin", Role.ADMIN, AccountStatus.ACTIVE.getValue(), null, null),
                        new AuthProfileResponse(false, false)
                ),
                "new-refresh-token",
                "refresh-family"
        ));

        Cookie refreshCookie = new Cookie("raktakk_refresh_token", "refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("raktakk_refresh_token=")));
    }

    @Test
    void registerShouldReturnAccessTokenAndRefreshCookie() throws Exception {
        doNothing().when(rateLimitService).checkAuthLimit(anyString());
        when(authService.register(org.mockito.ArgumentMatchers.any(com.raktakk.backend.dto.RegisterRequest.class)))
                .thenReturn(new AuthService.AuthBundle(
                        new AuthResponse(
                                "register-token",
                                new UserMeResponse(2L, "rate0@example.com", "Rate User 0", Role.USER, AccountStatus.ACTIVE.getValue(), null, null),
                                new AuthProfileResponse(false, false)
                        ),
                        "register-refresh-token",
                        "register-family"
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "rate0@example.com",
                                "password", "Password@123",
                                "fullName", "Rate User 0"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("raktakk_refresh_token=")));
    }
}
