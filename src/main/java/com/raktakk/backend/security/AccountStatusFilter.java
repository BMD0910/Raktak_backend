package com.raktakk.backend.security;

import com.raktakk.backend.entity.AccountStatus;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

@Component
@Order(100)
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public AccountStatusFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // nothing to check for anonymous or unauthenticated requests
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // allow read-only requests and OPTIONS/HEAD
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // allow public auth endpoints (login/register) so suspended users can obtain tokens
        if (path != null && path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // allow users with admin role to perform mutative operations
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(username).orElse(null);

        if (user != null && user.getAccountStatus() != null && user.getAccountStatus() != AccountStatus.ACTIVE) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            String msg = "{\"ok\":false,\"error\":\"ACCOUNT_SUSPENDED\",\"message\":\"Account is suspended or inactive\"}";
            response.getWriter().write(msg);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
