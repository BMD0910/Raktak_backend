package com.raktakk.backend.service;

import com.raktakk.backend.entity.AuthProvider;
import com.raktakk.backend.entity.Category;
import com.raktakk.backend.entity.SiteSettings;
import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.Role;
import com.raktakk.backend.entity.ServiceOffer;
import com.raktakk.backend.entity.SubscriptionPlan;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.repository.CategoryRepository;
import com.raktakk.backend.repository.ProfileRepository;
import com.raktakk.backend.repository.ServiceOfferRepository;
import com.raktakk.backend.repository.SiteSettingsRepository;
import com.raktakk.backend.repository.SubscriptionPlanRepository;
import com.raktakk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedSiteSettings();
        seedCategories();
        seedAdmin();
        cleanupLegacyDemoVendors();
        seedDemoUsers();
        seedProfiles();
        seedDemoVendorServices();
        seedSubscriptionPlans();
    }

    private void seedCategories() {
        List<Category> categories = List.of(
                Category.builder().name("Marketing Digital").slug("marketing-digital").icon("rocket").displayOrder(1).active(true).build(),
                Category.builder().name("BTP & Construction").slug("btp-construction").icon("building").displayOrder(2).active(true).build(),
                Category.builder().name("Informatique & Tech").slug("informatique-tech").icon("laptop-code").displayOrder(3).active(true).build(),
                Category.builder().name("Restauration & Traiteur").slug("restauration-traiteur").icon("utensils").displayOrder(4).active(true).build(),
                Category.builder().name("Immobilier").slug("immobilier").icon("house").displayOrder(5).active(true).build(),
                Category.builder().name("Beauté & Bien-être").slug("beaute-bien-etre").icon("spa").displayOrder(6).active(true).build()
        );

        for (Category seed : categories) {
            categoryRepository.findBySlugIgnoreCase(seed.getSlug()).ifPresentOrElse(existing -> {
                existing.setName(seed.getName());
                existing.setIcon(seed.getIcon());
                existing.setDisplayOrder(seed.getDisplayOrder());
                existing.setActive(true);
                categoryRepository.save(existing);
            }, () -> categoryRepository.save(seed));
        }
    }

    private void seedAdmin() {
        String email = "admin@raktakk.com";
        String plainPassword = "Admin@12345";
        User admin = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (admin == null) {
            userRepository.save(User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(plainPassword))
                    .fullName("Super Admin")
                    .role(Role.ADMIN)
                    .authProvider(AuthProvider.LOCAL)
                    .enabled(true)
                    .build());
            return;
        }

        admin.setFullName("Super Admin");
        admin.setRole(Role.ADMIN);
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setEnabled(true);
        admin.setAccountStatus(com.raktakk.backend.entity.AccountStatus.ACTIVE);
        if (!passwordEncoder.matches(plainPassword, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(plainPassword));
        }
        userRepository.save(admin);
    }

    private void seedSiteSettings() {
        SiteSettings settings = siteSettingsRepository.findFirstByOrderByUpdatedAtDesc().orElse(null);
        if (settings == null) {
            siteSettingsRepository.save(SiteSettings.builder()
                    .siteName("Raktakk")
                    .supportEmail("support@raktakk.com")
                    .supportPhone("+221 77 000 00 00")
                    .maintenanceMode(false)
                    .maintenanceMessage("")
                    .auditRetentionDays(90)
                    .allowNewRegistrations(true)
                    .allowNewVendorApplications(true)
                    .build());
            return;
        }

        boolean changed = false;
        if (settings.getSiteName() == null || settings.getSiteName().isBlank()) {
            settings.setSiteName("Raktakk");
            changed = true;
        }
        if (settings.getSupportEmail() == null || settings.getSupportEmail().isBlank()) {
            settings.setSupportEmail("support@raktakk.com");
            changed = true;
        }
        if (settings.getSupportPhone() == null || settings.getSupportPhone().isBlank()) {
            settings.setSupportPhone("+221 77 000 00 00");
            changed = true;
        }
        if (settings.getAuditRetentionDays() == null) {
            settings.setAuditRetentionDays(90);
            changed = true;
        }
        if (settings.getAllowNewRegistrations() == null) {
            settings.setAllowNewRegistrations(true);
            changed = true;
        }
        if (settings.getAllowNewVendorApplications() == null) {
            settings.setAllowNewVendorApplications(true);
            changed = true;
        }
        if (settings.getMaintenanceMessage() == null) {
            settings.setMaintenanceMessage("");
            changed = true;
        }

        if (changed) {
            siteSettingsRepository.save(settings);
        }
    }

    private void seedDemoUsers() {
        for (DemoVendorSeed seed : demoVendorSeeds()) {
            if (userRepository.existsByEmailIgnoreCase(seed.email())) {
                continue;
            }
            userRepository.save(User.builder()
                    .email(seed.email())
                    .password(passwordEncoder.encode("Vendor@12345"))
                    .fullName(seed.companyName())
                    .role(Role.USER)
                    .authProvider(AuthProvider.LOCAL)
                    .enabled(true)
                    .build());
        }
    }

    private void seedProfiles() {
        for (DemoVendorSeed seed : demoVendorSeeds()) {
            User user = userRepository.findByEmailIgnoreCase(seed.email()).orElse(null);
            if (user == null) {
                continue;
            }

            var existingProfile = profileRepository.findByUserId(user.getId());
            if (existingProfile.isPresent()) {
                Profile profile = existingProfile.get();
                applyVendorSeed(profile, seed);
                profileRepository.save(profile);
                continue;
            }

            profileRepository.save(Profile.builder()
                    .user(user)
                    .isVendor(true)
                    .vendorVerified(true)
                    .subscriptionActive(true)
                    .profileCompleted(true)
                    .subscriptionPlanCode(seed.subscriptionPlanCode())
                    .profession(seed.profession())
                    .skills(seed.skills())
                    .experience(seed.experience())
                    .description(seed.description())
                    .location(seed.location())
                    .phone(seed.phone())
                    .avatar(seed.avatar())
                    .rating(seed.rating())
                    .totalReviews(seed.totalReviews())
                    .build());
        }
    }

    private void seedDemoVendorServices() {
        for (DemoVendorSeed seed : demoVendorSeeds()) {
            User user = userRepository.findByEmailIgnoreCase(seed.email()).orElse(null);
            if (user == null) continue;

            Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
            if (profile == null || !profile.isVendor()) continue;

            if (!serviceOfferRepository.findByVendorProfileUserIdOrderByIdDesc(user.getId()).isEmpty()) {
                continue;
            }

            List<ServiceOffer> offers = seed.services().stream()
                    .map(service -> ServiceOffer.builder()
                            .title(service.title())
                            .description(service.description())
                            .price(service.price())
                            .vendorProfile(profile)
                            .category(seed.category())
                            .imageUrl(null)
                            .deliveryTime(service.deliveryTime())
                            .featured(service.featured())
                            .active(true)
                            .build())
                    .toList();

            serviceOfferRepository.saveAll(offers);
        }
    }

    private void cleanupLegacyDemoVendors() {
        List<String> legacyEmails = List.of(
                "vendor1@raktakk.com",
                "vendor2@raktakk.com",
                "vendor3@raktakk.com",
                "vendor4@raktakk.com",
                "vendor5@raktakk.com",
                "vendor6@raktakk.com"
        );

        for (String email : legacyEmails) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                serviceOfferRepository.findByVendorProfileUserIdOrderByIdDesc(user.getId())
                        .forEach(serviceOfferRepository::delete);
                profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);
                userRepository.delete(user);
            });
        }
    }

    private void applyVendorSeed(Profile profile, DemoVendorSeed seed) {
        profile.setVendor(true);
        profile.setVendorVerified(true);
        profile.setSubscriptionActive(true);
        profile.setProfileCompleted(true);
        profile.setSubscriptionPlanCode(seed.subscriptionPlanCode());
        profile.setProfession(seed.profession());
        profile.setSkills(seed.skills());
        profile.setExperience(seed.experience());
        profile.setDescription(seed.description());
        profile.setLocation(seed.location());
        profile.setPhone(seed.phone());
        profile.setAvatar(seed.avatar());
        profile.setRating(seed.rating());
        profile.setTotalReviews(seed.totalReviews());
    }

    private List<DemoVendorSeed> demoVendorSeeds() {
        return List.of(
                new DemoVendorSeed(
                        "aida.ndiaye@raktakk.test",
                        "Ndiaye Digital Studio",
                        "Marketing Digital",
                        "Consultante marketing digital",
                        "SEO, publicité Meta, création de contenu, community management",
                        "6 ans d'expérience en stratégie digitale pour PME et marques locales",
                        "Agence digitale basée à Dakar, spécialisée dans le lancement de campagnes et la création de contenu performant.",
                        "Dakar, Sénégal",
                        "+221770000101",
                        "rocket",
                        "PRO",
                        4.9,
                        128,
                        List.of(
                                new DemoServiceSeed("Pack site vitrine", "Site vitrine responsive avec pages essentielles et optimisation SEO de base.", 180000.0, 7, true),
                                new DemoServiceSeed("Campagne Meta Ads", "Mise en place et gestion de campagnes Facebook/Instagram orientées conversion.", 120000.0, 5, false),
                                new DemoServiceSeed("Community management", "Animation des réseaux sociaux avec calendrier éditorial et reporting mensuel.", 90000.0, 30, false)
                        )
                ),
                new DemoVendorSeed(
                        "ibrahima.diop@raktakk.test",
                        "Diop BTP Services",
                        "BTP & Construction",
                        "Chef de chantier",
                        "Maçonnerie, rénovation, carrelage, suivi de chantier",
                        "10 ans d'expérience sur des projets résidentiels et commerciaux à Thiès et Dakar",
                        "Entreprise de construction et rénovation pour maisons, boutiques et petits immeubles.",
                        "Thiès, Sénégal",
                        "+221770000202",
                        "building",
                        "BUSINESS",
                        4.7,
                        94,
                        List.of(
                                new DemoServiceSeed("Rénovation intérieure", "Réfection complète d'intérieur avec peinture, enduit et petite maçonnerie.", 350000.0, 14, true),
                                new DemoServiceSeed("Pose de carrelage", "Pose soignée de carrelage sol et mur pour cuisine, salle de bain ou terrasse.", 200000.0, 8, false),
                                new DemoServiceSeed("Suivi de chantier", "Pilotage et coordination des travaux avec rapport d'avancement régulier.", 150000.0, 21, false)
                        )
                ),
                new DemoVendorSeed(
                        "fatou.sarr@raktakk.test",
                        "Aïcha Event Design",
                        "Événementiel",
                        "Organisatrice d'événements",
                        "Décoration, coordination, logistique, sonorisation",
                        "8 ans d'expérience dans l'organisation de mariages, anniversaires et événements d'entreprise",
                        "Prestataire événementiel spécialisé dans les cérémonies élégantes et les prestations clé en main.",
                        "Abidjan, Côte d'Ivoire",
                        "+225010000303",
                        "sparkles",
                        "PRO",
                        4.8,
                        211,
                        List.of(
                                new DemoServiceSeed("Décoration mariage", "Décoration complète de salle et d'espace extérieur avec thème personnalisé.", 450000.0, 10, true),
                                new DemoServiceSeed("Coordination événement", "Gestion des prestataires, du timing et des imprévus le jour J.", 220000.0, 14, false),
                                new DemoServiceSeed("Pack sonorisation", "Location et installation de matériel son et micro pour petits et moyens événements.", 130000.0, 3, false)
                        )
                )
        );
    }

    private record DemoServiceSeed(String title, String description, Double price, Integer deliveryTime, Boolean featured) {}

    private record DemoVendorSeed(
            String email,
            String companyName,
            String category,
            String profession,
            String skills,
            String experience,
            String description,
            String location,
            String phone,
            String avatar,
            String subscriptionPlanCode,
            Double rating,
            Integer totalReviews,
            List<DemoServiceSeed> services
    ) {}

    private void seedSubscriptionPlans() {
        List<SubscriptionPlan> plans = List.of(
                SubscriptionPlan.builder()
                        .code("BASIC")
                        .name("Gratuit")
                        .priceFcfa(0L)
                        .description("Pour découvrir la plateforme avant abonnement vendeur")
                        .featuresText("Profil basique\n10 leads/mois")
                        .active(true)
                        .displayOrder(1)
                        .build(),
                SubscriptionPlan.builder()
                        .code("PRO")
                        .name("Pro")
                        .priceFcfa(29900L)
                        .description("Pour devenir prestataire avec visibilité renforcée")
                        .featuresText("Profil complet\n50 leads/mois")
                        .active(true)
                        .displayOrder(2)
                        .build(),
                SubscriptionPlan.builder()
                        .code("BUSINESS")
                        .name("Business")
                        .priceFcfa(79900L)
                        .description("Pour les structures qui veulent un volume maximal")
                        .featuresText("Leads illimités\nMise en avant")
                        .active(true)
                        .displayOrder(3)
                        .build()
        );

        for (SubscriptionPlan seed : plans) {
            subscriptionPlanRepository.findByCodeIgnoreCase(seed.getCode()).ifPresentOrElse(existing -> {
                existing.setName(seed.getName());
                existing.setPriceFcfa(seed.getPriceFcfa());
                existing.setDescription(seed.getDescription());
                existing.setFeaturesText(seed.getFeaturesText());
                existing.setActive(seed.isActive());
                existing.setDisplayOrder(seed.getDisplayOrder());
                subscriptionPlanRepository.save(existing);
            }, () -> subscriptionPlanRepository.save(seed));
        }
    }
}
