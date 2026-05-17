package com.raktakk.backend.security;

import com.raktakk.backend.entity.AuthProvider;
import com.raktakk.backend.entity.Role;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth provider");
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .fullName(name == null ? "Google User" : name)
                        .password("oauth2-user")
                        .role(Role.USER)
                        .authProvider(AuthProvider.GOOGLE)
                        .enabled(true)
                        .build()));

        String token = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        String redirect = successRedirectUri + "?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }
}
