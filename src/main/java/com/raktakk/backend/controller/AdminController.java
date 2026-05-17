package com.raktakk.backend.controller;

import com.raktakk.backend.entity.*;
import com.raktakk.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrderRepository orderRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${mail.admin:admin@raktakk.com}")
    private String defaultAdminEmail;

    @GetMapping("/stats.php")
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long suspendedUsers = userRepository.countByAccountStatus(AccountStatus.SUSPENDED) + userRepository.countByAccountStatus(AccountStatus.INACTIVE);

        long vendorsCount = profileRepository.countByIsVendorTrue();
        long clientsCount = profileRepository.countByIsVendorFalse();
        long verifiedVendors = profileRepository.findByIsVendorTrueAndVendorVerifiedTrue().size();

        long servicesCount = serviceOfferRepository.count();
        long suspendedServicesCount = serviceOfferRepository.countByStatus(ServiceStatus.SUSPENDED) + serviceOfferRepository.countByStatus(ServiceStatus.INACTIVE);

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.COMPLETED);

        java.time.Instant now = java.time.Instant.now();
        java.time.Instant weekAgo = now.minus(java.time.Duration.ofDays(7));
        long newUsers7Days = userRepository.countByCreatedAtAfter(weekAgo);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers);
        data.put("suspendedUsers", suspendedUsers);
        data.put("vendorsCount", vendorsCount);
        data.put("verifiedVendors", verifiedVendors);
        data.put("clientsCount", clientsCount);
        data.put("servicesCount", servicesCount);
        data.put("suspendedServicesCount", suspendedServicesCount);
        data.put("totalOrders", totalOrders);
        data.put("pendingOrders", pendingOrders);
        data.put("completedOrders", completedOrders);
        data.put("newUsers7Days", newUsers7Days);

        return Map.of("ok", true, "data", data);
    }

    @GetMapping("/settings.php")
    @Transactional(readOnly = true)
    public Map<String, Object> settings() {
        SiteSettings settings = getOrCreateSettings();
        return Map.of("ok", true, "data", mapSettings(settings));
    }

    @PostMapping("/settings-save.php")
    public Map<String, Object> saveSettings(@RequestBody Map<String, Object> body, Authentication authentication) {
        SiteSettings settings = getOrCreateSettings();
        Map<String, Object> before = mapSettings(settings);

        if (body.get("siteName") != null) settings.setSiteName(String.valueOf(body.get("siteName")).trim());
        if (body.get("supportEmail") != null) settings.setSupportEmail(String.valueOf(body.get("supportEmail")).trim());
        if (body.get("supportPhone") != null) settings.setSupportPhone(String.valueOf(body.get("supportPhone")).trim());
        if (body.get("maintenanceMode") != null) settings.setMaintenanceMode(Boolean.parseBoolean(String.valueOf(body.get("maintenanceMode"))));
        if (body.get("maintenanceMessage") != null) settings.setMaintenanceMessage(String.valueOf(body.get("maintenanceMessage")).trim());
        if (body.get("auditRetentionDays") != null) settings.setAuditRetentionDays(Integer.parseInt(String.valueOf(body.get("auditRetentionDays"))));
        if (body.get("allowNewRegistrations") != null) settings.setAllowNewRegistrations(Boolean.parseBoolean(String.valueOf(body.get("allowNewRegistrations"))));
        if (body.get("allowNewVendorApplications") != null) settings.setAllowNewVendorApplications(Boolean.parseBoolean(String.valueOf(body.get("allowNewVendorApplications"))));

        siteSettingsRepository.save(settings);

        logAudit(authentication, "admin_settings_update", "settings", settings.getId(), "Paramètres admin mis à jour", Map.of(
                "siteName", settings.getSiteName(),
                "maintenanceMode", settings.isMaintenanceMode(),
                "auditRetentionDays", settings.getAuditRetentionDays(),
                "changedFields", diffSettingFields(before, mapSettings(settings))
        ));

        return Map.of("ok", true, "message", "Paramètres mis à jour", "data", mapSettings(settings));
    }

    @GetMapping("/users.php")
    @Transactional(readOnly = true)
    public Map<String, Object> users(@RequestParam(required = false) String role,
                         @RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int limit) {
        String query = q == null ? "" : q.trim().toLowerCase();

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "id"));
        Page<User> usersPage = userRepository.findAll(pageable);

        List<Map<String, Object>> content = usersPage.getContent().stream()
            .filter(user -> matchesRole(user, role))
            .filter(user -> matchesQuery(user, query))
            .map(this::mapUser)
            .toList();

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", usersPage.getNumber() + 1);
        pagination.put("limit", usersPage.getSize());
        pagination.put("total", usersPage.getTotalElements());
        pagination.put("pages", usersPage.getTotalPages());

        return Map.of("ok", true, "data", content, "pagination", pagination);
    }

    @PostMapping("/users-status.php")
    public Map<String, Object> updateUserStatus(@RequestBody Map<String, Object> body, Authentication authentication) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        String statusRaw = body.get("account_status") == null ? "active" : String.valueOf(body.get("account_status"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason")).trim();
        Object contact = body.get("contact");

        if (id == null) {
            return Map.of("ok", false, "message", "Paramètre id requis");
        }

        User user = userRepository.findById(id).orElseThrow();
        AccountStatus status = AccountStatus.fromValue(statusRaw);
        user.setAccountStatus(status);
        // Do not toggle `enabled` here — we allow suspended users to authenticate
        // but the frontend will restrict access based on `accountStatus`.

        if (status == AccountStatus.ACTIVE) {
            user.setDeactivationReason(null);
            user.setDeactivationContact(null);
            user.setDeactivatedAt(null);
            user.setDeactivatedBy(null);
        } else {
            user.setDeactivationReason(reason);
            user.setDeactivationContact(contact == null ? null : contact.toString());
            user.setDeactivatedAt(Instant.now());
            user.setDeactivatedBy(null);
        }

        userRepository.save(user);
    logAudit(authentication, status == AccountStatus.ACTIVE ? "user_reactivate" : "user_suspend", "user", user.getId(), reason, Map.of(
        "account_status", status.name().toLowerCase(),
        "contact", user.getDeactivationContact() == null ? "" : user.getDeactivationContact()
    ));

        return Map.of("ok", true, "message", "Statut mis à jour", "data", mapUser(user));
    }

    @GetMapping("/services.php")
    @Transactional(readOnly = true)
    public Map<String, Object> services(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int limit) {
        String q = search == null ? "" : search.trim().toLowerCase();

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "id"));
        Page<ServiceOffer> servicesPage = serviceOfferRepository.findAll(pageable);

        List<Map<String, Object>> content = servicesPage.getContent().stream()
                .filter(service -> status == null || status.isBlank() || service.getStatus().name().equalsIgnoreCase(status))
                .filter(service -> q.isBlank() || contains(service.getTitle(), q) || contains(service.getDescription(), q))
                .map(this::mapService)
                .collect(Collectors.toList());

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", servicesPage.getNumber() + 1);
        pagination.put("limit", servicesPage.getSize());
        pagination.put("total", servicesPage.getTotalElements());
        pagination.put("pages", servicesPage.getTotalPages());

        return Map.of("ok", true, "data", content, "pagination", pagination);
    }

    @GetMapping("/metrics.php")
    @Transactional(readOnly = true)
    public Map<String, Object> metrics() {
        // Users trend for last 14 days
        List<Map<String, Object>> usersTrend = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        for (int i = 13; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            java.time.Instant start = day.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            java.time.Instant end = day.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            long count = userRepository.countByCreatedAtBetween(start, end);
            usersTrend.add(Map.of("date", day.toString(), "count", count));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("usersTrend", usersTrend);
        return Map.of("ok", true, "data", data);
    }

    @GetMapping("/requests.php")
    @Transactional(readOnly = true)
    public Map<String, Object> requests(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int limit) {
        String q = search == null ? "" : search.trim().toLowerCase();

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> requestsPage = orderRepository.findAll(pageable);

        List<Map<String, Object>> content = requestsPage.getContent().stream()
                .filter(order -> status == null || status.isBlank() || order.getStatus().name().equalsIgnoreCase(status))
                .filter(order -> q.isBlank() || contains(order.getService().getTitle(), q) 
                    || contains(order.getClient().getEmail(), q)
                    || contains(order.getVendor().getEmail(), q))
                .map(this::mapRequest)
                .collect(Collectors.toList());

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", requestsPage.getNumber() + 1);
        pagination.put("limit", requestsPage.getSize());
        pagination.put("total", requestsPage.getTotalElements());
        pagination.put("pages", requestsPage.getTotalPages());

        return Map.of("ok", true, "data", content, "pagination", pagination);
    }

    @PostMapping("/services-status.php")
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> updateServiceStatus(@RequestBody Map<String, Object> body, Authentication authentication) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        String statusRaw = body.get("status") == null ? "active" : String.valueOf(body.get("status"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason")).trim();
        if (id == null) {
            return Map.of("ok", false, "message", "Paramètre id requis");
        }

        ServiceOffer service = serviceOfferRepository.findById(id).orElseThrow();
        ServiceStatus status = ServiceStatus.fromValue(statusRaw);

        if (status != ServiceStatus.ACTIVE && (reason == null || reason.isBlank())) {
            return Map.of("ok", false, "message", "Le motif est obligatoire pour désactiver un service");
        }

        service.setStatus(status);
        service.setActive(status == ServiceStatus.ACTIVE);

        if (status == ServiceStatus.ACTIVE) {
            service.setDeactivationReason(null);
            service.setDeactivatedAt(null);
            service.setDeactivatedBy(null);
        } else {
            service.setDeactivationReason(reason);
            service.setDeactivatedAt(Instant.now());
            service.setDeactivatedBy(null);
        }

        serviceOfferRepository.save(service);
    logAudit(authentication, status == ServiceStatus.ACTIVE ? "service_reactivate" : "service_suspend", "service", service.getId(), reason, Map.of(
        "status", status.name().toLowerCase(),
        "title", service.getTitle()
    ));
        return Map.of("ok", true, "message", "Statut du service mis à jour", "data", mapService(service));
    }

    @GetMapping("/audit-logs.php")
    @Transactional(readOnly = true)
    public Map<String, Object> auditLogs(@RequestParam(required = false) Long admin_id,
                                         @RequestParam(required = false) String target_type,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int limit) {
        Instant fromDate = parseInstant(from);
        Instant toDate = parseInstant(to);
        List<AuditLog> filtered = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(log -> admin_id == null || Objects.equals(log.getAdminId(), admin_id))
            .filter(log -> target_type == null || target_type.isBlank() || target_type.equalsIgnoreCase(log.getTargetType()))
            .filter(log -> action == null || action.isBlank() || action.equalsIgnoreCase(log.getAction()))
            .filter(log -> fromDate == null || !log.getCreatedAt().isBefore(fromDate))
            .filter(log -> toDate == null || !log.getCreatedAt().isAfter(toDate))
            .toList();

        int safePage = Math.max(page - 1, 0);
        int safeLimit = Math.min(limit, 100);
        int fromIndex = Math.min(safePage * safeLimit, filtered.size());
        int toIndex = Math.min(fromIndex + safeLimit, filtered.size());
        List<AuditLog> pageItems = filtered.subList(fromIndex, toIndex);

        List<Map<String, Object>> data = pageItems.stream().map(this::mapAudit).toList();
        int totalPages = safeLimit == 0 ? 0 : (int) Math.ceil(filtered.size() / (double) safeLimit);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", safePage + 1);
        pagination.put("limit", safeLimit);
        pagination.put("total", filtered.size());
        pagination.put("pages", totalPages);

        return Map.of("ok", true, "data", data, "pagination", pagination);
    }

    private Map<String, Object> mapUser(User user) {
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("username", user.getEmail().split("@")[0]);
        map.put("role", profile != null && profile.isVendor() ? "vendor" : "client");
        if (user.getRole() == Role.ADMIN) {
            map.put("role", "admin");
        }
        map.put("full_name", user.getFullName());
        map.put("company_name", profile != null && profile.isVendor() ? user.getFullName() : null);
        map.put("phone", profile != null ? profile.getPhone() : null);
        map.put("city", profile != null ? profile.getLocation() : null);
        map.put("country", "Sénégal");
        map.put("plan", profile != null && Boolean.TRUE.equals(profile.getSubscriptionActive()) ? profile.getSubscriptionPlanCode() : "free");
        map.put("account_status", user.getAccountStatus() == null ? "active" : user.getAccountStatus().name().toLowerCase());
        map.put("deactivation_reason", user.getDeactivationReason());
        map.put("deactivation_contact", user.getDeactivationContact());
        map.put("deactivated_at", user.getDeactivatedAt());
        map.put("deactivated_by", user.getDeactivatedBy());
        map.put("created_at", user.getCreatedAt());
        map.put("updated_at", user.getUpdatedAt());
        return map;
    }

    private Map<String, Object> mapService(ServiceOffer service) {
        Profile profile = service.getVendorProfile();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", service.getId());
        map.put("title", service.getTitle());
        map.put("description", service.getDescription());
        map.put("price", service.getPrice());
        map.put("price_range", service.getPrice());
        map.put("category", service.getCategory());
        map.put("category_name", service.getCategory());
        map.put("imageUrl", service.getImageUrl());
        map.put("deliveryTime", service.getDeliveryTime());
        map.put("featured", service.getFeatured());
        map.put("vendorId", profile != null && profile.getUser() != null ? profile.getUser().getId() : null);
        map.put("vendorName", profile != null && profile.getUser() != null ? profile.getUser().getFullName() : null);
        map.put("full_name", profile != null && profile.getUser() != null ? profile.getUser().getFullName() : null);
        map.put("email", profile != null && profile.getUser() != null ? profile.getUser().getEmail() : null);
        map.put("vendorVerified", profile != null && profile.isVendorVerified());
        map.put("active", service.isActive());
        map.put("status", service.getStatus() == null ? (service.isActive() ? "active" : "inactive") : service.getStatus().name().toLowerCase());
        map.put("statusLabel", service.getStatus() == null ? (service.isActive() ? "Actif" : "Inactif") : switch (service.getStatus()) {
            case ACTIVE -> "Actif";
            case SUSPENDED -> "Suspendu";
            case INACTIVE -> "Inactif";
        });
        map.put("deactivation_reason", service.getDeactivationReason());
        map.put("deactivated_at", service.getDeactivatedAt());
        map.put("created_at", null);
        return map;
    }

    private Map<String, Object> mapRequest(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("client_id", order.getClient() != null ? order.getClient().getId() : null);
        map.put("client_email", order.getClient() != null ? order.getClient().getEmail() : null);
        map.put("client_name", order.getClient() != null ? order.getClient().getFullName() : null);
        map.put("vendor_id", order.getVendor() != null ? order.getVendor().getId() : null);
        map.put("vendor_email", order.getVendor() != null ? order.getVendor().getEmail() : null);
        map.put("vendor_name", order.getVendor() != null ? order.getVendor().getFullName() : null);
        map.put("service_id", order.getService() != null ? order.getService().getId() : null);
        map.put("service_title", order.getService() != null ? order.getService().getTitle() : null);
        map.put("status", order.getStatus() == null ? "pending" : order.getStatus().name().toLowerCase());
        map.put("description", order.getDescription());
        map.put("created_at", order.getCreatedAt());
        map.put("updated_at", order.getUpdatedAt());
        return map;
    }

    private Map<String, Object> mapAudit(AuditLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("admin_id", log.getAdminId());
        map.put("action", log.getAction());
        map.put("target_type", log.getTargetType());
        map.put("target_id", log.getTargetId());
        map.put("reason", log.getReason());
        map.put("extra", log.getExtra());
        map.put("created_at", log.getCreatedAt());
        return map;
    }

    private SiteSettings getOrCreateSettings() {
        return siteSettingsRepository.findFirstByOrderByUpdatedAtDesc().orElseGet(() -> siteSettingsRepository.save(SiteSettings.builder()
                .siteName("Raktakk")
            .supportEmail(defaultAdminEmail)
                .supportPhone("+221 77 000 00 00")
                .maintenanceMode(false)
                .maintenanceMessage("")
                .auditRetentionDays(90)
                .allowNewRegistrations(true)
                .allowNewVendorApplications(true)
                .build()));
    }

    private Map<String, Object> mapSettings(SiteSettings settings) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", settings.getId());
        map.put("siteName", settings.getSiteName());
        map.put("supportEmail", settings.getSupportEmail());
        map.put("supportPhone", settings.getSupportPhone());
        map.put("maintenanceMode", settings.isMaintenanceMode());
        map.put("maintenanceMessage", settings.getMaintenanceMessage());
        map.put("auditRetentionDays", settings.getAuditRetentionDays());
        map.put("allowNewRegistrations", settings.getAllowNewRegistrations());
        map.put("allowNewVendorApplications", settings.getAllowNewVendorApplications());
        map.put("createdAt", settings.getCreatedAt());
        map.put("updatedAt", settings.getUpdatedAt());
        return map;
    }

    private List<String> diffSettingFields(Map<String, Object> before, Map<String, Object> after) {
        List<String> changed = new ArrayList<>();
        for (String key : after.keySet()) {
            if (!Objects.equals(before.get(key), after.get(key))) changed.add(key);
        }
        return changed;
    }

    private void logAudit(Authentication authentication, String action, String targetType, Long targetId, String reason, Map<String, Object> extra) {
        Long adminId = null;
        if (authentication != null && authentication.getName() != null) {
            adminId = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .map(User::getId)
                    .orElse(null);
        }

        String extraJson = null;
        if (extra != null && !extra.isEmpty()) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
                Object value = entry.getValue();
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append('"').append(escapeJson(String.valueOf(value))).append('"');
                }
            }
            sb.append('}');
            extraJson = sb.toString();
        }

        auditLogRepository.save(AuditLog.builder()
                .adminId(adminId == null ? 0L : adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .extra(extraJson)
                .build());
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private boolean matchesRole(User user, String role) {
        if (role == null || role.isBlank()) return true;
        if ("admin".equalsIgnoreCase(role)) return user.getRole() == Role.ADMIN;
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if ("vendor".equalsIgnoreCase(role)) return profile != null && profile.isVendor();
            if ("client".equalsIgnoreCase(role)) return (profile == null || !profile.isVendor()) && user.getRole() != Role.ADMIN;
        return true;
    }

    // ========== ADMIN MANAGEMENT ENDPOINTS ==========

    @GetMapping("/admins.php")
    @Transactional(readOnly = true)
    public Map<String, Object> listAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<User> admins = userRepository.findByRole(Role.ADMIN, pageable);
        
        // Exclure l'admin principal (admin@raktakk.com) de la modification
        List<Map<String, Object>> adminsList = admins.stream()
                .map(admin -> {
                    Map<String, Object> map = mapAdmin(admin);
                    map.put("isProtected", "admin@raktakk.com".equals(admin.getEmail()));
                    return map;
                })
                .collect(Collectors.toList());
        
        return Map.of(
                "ok", true,
                "data", adminsList,
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", admins.getTotalElements(),
                        "pages", admins.getTotalPages()
                )
        );
    }

    @PostMapping("/admins-create.php")
    @Transactional
    public Map<String, Object> createAdmin(@RequestBody Map<String, Object> body, Authentication authentication) {
        String email = body.get("email") != null ? body.get("email").toString().trim() : null;
        String fullName = body.get("fullName") != null ? body.get("fullName").toString().trim() : null;
        String password = body.get("password") != null ? body.get("password").toString().trim() : null;

        if (email == null || email.isBlank() || fullName == null || fullName.isBlank() || password == null || password.isBlank()) {
            return Map.of("ok", false, "error", "Email, nom et mot de passe requis");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return Map.of("ok", false, "error", "Cet email existe déjà");
        }

        User admin = User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        User saved = userRepository.save(admin);
        logAudit(authentication, "admin_create", "admin", saved.getId(), 
                "Admin créé: " + email, Map.of("email", email, "fullName", fullName));

        return Map.of(
                "ok", true,
                "message", "Admin créé avec succès",
                "data", mapAdmin(saved)
        );
    }

    @PostMapping("/admins-update.php")
    @Transactional
    public Map<String, Object> updateAdmin(@RequestBody Map<String, Object> body, Authentication authentication) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return Map.of("ok", false, "error", "ID requis");
        }
        Long adminId = Long.parseLong(idObj.toString());
        
        User admin = userRepository.findById(adminId)
                .orElse(null);
        if (admin == null) {
            return Map.of("ok", false, "error", "Admin non trouvé");
        }

        // Protection: admin@raktakk.com ne peut pas être modifié
        if ("admin@raktakk.com".equals(admin.getEmail())) {
            return Map.of("ok", false, "error", "Cet admin ne peut pas être modifié");
        }

        Map<String, Object> before = mapAdmin(admin);
        
        if (body.containsKey("fullName")) {
            admin.setFullName(body.get("fullName").toString().trim());
        }
        if (body.containsKey("email")) {
            String newEmail = body.get("email").toString().trim();
            if (!newEmail.equals(admin.getEmail()) && userRepository.findByEmail(newEmail).isPresent()) {
                return Map.of("ok", false, "error", "Cet email existe déjà");
            }
            admin.setEmail(newEmail);
        }
        if (body.containsKey("password")) {
            String newPassword = body.get("password").toString().trim();
            if (!newPassword.isBlank()) {
                admin.setPassword(passwordEncoder.encode(newPassword));
            }
        }
        if (body.containsKey("enabled")) {
            admin.setEnabled(Boolean.parseBoolean(body.get("enabled").toString()));
        }

        User updated = userRepository.save(admin);
        logAudit(authentication, "admin_update", "admin", updated.getId(), 
                "Admin modifié: " + admin.getEmail(), Map.of("changes", diffMaps(before, mapAdmin(updated))));

        return Map.of(
                "ok", true,
                "message", "Admin mis à jour avec succès",
                "data", mapAdmin(updated)
        );
    }

    @PostMapping("/admins-delete.php")
    @Transactional
    public Map<String, Object> deleteAdmin(@RequestBody Map<String, Object> body, Authentication authentication) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return Map.of("ok", false, "error", "ID requis");
        }
        Long adminId = Long.parseLong(idObj.toString());
        
        User admin = userRepository.findById(adminId)
                .orElse(null);
        if (admin == null) {
            return Map.of("ok", false, "error", "Admin non trouvé");
        }

        // Protection: admin@raktakk.com ne peut pas être supprimé ou suspendu
        if ("admin@raktakk.com".equals(admin.getEmail())) {
            return Map.of("ok", false, "error", "Cet admin ne peut pas être supprimé");
        }

        String reason = body.get("reason") != null ? body.get("reason").toString().trim() : "Suspension sans motif";
        
        admin.setAccountStatus(AccountStatus.SUSPENDED);
        admin.setDeactivationReason(reason);
        admin.setDeactivatedAt(Instant.now());
        admin.setDeactivatedBy(extractAdminId(authentication));
        admin.setEnabled(false);

        User updated = userRepository.save(admin);
        logAudit(authentication, "admin_suspend", "admin", updated.getId(), 
                "Admin suspendu: " + reason, Map.of("email", admin.getEmail(), "reason", reason));

        return Map.of(
                "ok", true,
                "message", "Admin suspendu avec succès",
                "data", mapAdmin(updated)
        );
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> mapAdmin(User admin) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", admin.getId());
        map.put("email", admin.getEmail());
        map.put("fullName", admin.getFullName());
        map.put("enabled", admin.isEnabled());
        map.put("accountStatus", admin.getAccountStatus().toString());
        map.put("createdAt", admin.getCreatedAt().toString());
        map.put("updatedAt", admin.getUpdatedAt().toString());
        map.put("lastDeactivatedAt", admin.getDeactivatedAt() != null ? admin.getDeactivatedAt().toString() : null);
        map.put("deactivationReason", admin.getDeactivationReason());
        return map;
    }

    private Map<String, Object> diffMaps(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> diff = new HashMap<>();
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());
        
        for (String key : allKeys) {
            Object beforeVal = before.get(key);
            Object afterVal = after.get(key);
            if (!Objects.equals(beforeVal, afterVal)) {
                diff.put(key, Map.of("before", beforeVal, "after", afterVal));
            }
        }
        return diff;
    }

    private Long extractAdminId(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByEmailIgnoreCase(authentication.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    private boolean matchesQuery(User user, String q) {
        if (q == null || q.isBlank()) return true;
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return contains(user.getEmail(), q)
                || contains(user.getFullName(), q)
                || (profile != null && contains(profile.getLocation(), q))
                || (profile != null && contains(profile.getPhone(), q));
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
