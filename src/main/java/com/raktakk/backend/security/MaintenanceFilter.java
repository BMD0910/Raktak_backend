package com.raktakk.backend.security;

import com.raktakk.backend.entity.SiteSettings;
import com.raktakk.backend.repository.SiteSettingsRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Order(1)
public class MaintenanceFilter extends OncePerRequestFilter {

    private final SiteSettingsRepository siteSettingsRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Allow health and actuator endpoints, public API and OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/public/")
                || path.startsWith("/api/public/health")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<SiteSettings> opt = siteSettingsRepository.findFirstByOrderByUpdatedAtDesc();
        SiteSettings settings = opt.orElseGet(() -> SiteSettings.builder().maintenanceMode(false).maintenanceMessage("").build());

        if (!settings.isMaintenanceMode()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow certain admin/login paths so admins can authenticate and access dashboard
        if (path.startsWith("/api/auth") || path.startsWith("/api/admin") || path.startsWith("/admin") || path.startsWith("/dashboard") || path.endsWith("dashboard-admin.html") || path.startsWith("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow admins to access while in maintenance
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority g : auth.getAuthorities()) {
                if ("ROLE_ADMIN".equals(g.getAuthority())) {
                    isAdmin = true;
                    break;
                }
            }
        }

        if (isAdmin) {
            filterChain.doFilter(request, response);
            return;
        }

        String msg = settings.getMaintenanceMessage() == null || settings.getMaintenanceMessage().isBlank()
                ? "Site en maintenance"
                : settings.getMaintenanceMessage();

        // If request likely from a browser (HTML), return a styled HTML page; otherwise return JSON
        String accept = request.getHeader("Accept");
        boolean wantsHtml = accept != null && accept.contains("text/html");
        if (wantsHtml || !path.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/html;charset=UTF-8");
            String page = "<!doctype html>"
                    + "<html><head><meta charset=\"utf-8\"><title>Maintenance</title>"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<style>body{font-family:Inter, system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;background:#f5f7fb;color:#243142}"
                    + ".card{max-width:760px;margin:24px;padding:28px;border-radius:12px;background:linear-gradient(180deg,rgba(255,255,255,0.85),rgba(250,250,252,0.85));box-shadow:0 6px 30px rgba(34,50,84,0.12);border:1px solid rgba(34,50,84,0.06)}"
                    + ".title{font-size:20px;font-weight:700;margin-bottom:8px;color:#0b2b4a}.msg{font-size:15px;color:#324a5f;margin-bottom:14px}.hint{font-size:13px;color:#556679}.actions{margin-top:18px}</style></head><body>"
                    + "<div class=\"card\"><div class=\"title\">Site en maintenance</div>"
                    + "<div class=\"msg\">" + escapeHtml(msg) + "</div>"
                    + "<div class=\"hint\">Nous travaillons à l'amélioration du site. Seuls les administrateurs peuvent se connecter. Merci de votre patience.</div>"
                    + "</div></body></html>";
            response.getWriter().write(page);
            return;
        }

        // JSON fallback for API clients
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        String out = String.format("{\"ok\":false,\"message\":\"%s\"}", msg.replace("\"", "\\\""));
        response.getWriter().write(out);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
