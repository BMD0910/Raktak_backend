package com.raktakk.backend.service;

import com.raktakk.backend.dto.CityResponse;
import com.raktakk.backend.dto.ReviewResponse;
import com.raktakk.backend.dto.VendorResponse;
import com.raktakk.backend.entity.Category;
import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.Role;
import com.raktakk.backend.entity.ServiceOffer;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.repository.CategoryRepository;
import com.raktakk.backend.repository.ProfileRepository;
import com.raktakk.backend.repository.ServiceOfferRepository;
import com.raktakk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicDirectoryService {

    private static final List<String> CITIES = List.of("Dakar", "Abidjan", "Bamako", "Lomé", "Cotonou", "Conakry", "Thiès", "Saint-Louis");
    private static final List<String> EMOJIS = List.of("rocket", "building", "laptop-code", "utensils", "house", "spa", "gift", "camera", "balance-scale", "truck");

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;
    private final ServiceOfferRepository serviceOfferRepository;

    @Transactional(readOnly = true)
    public List<VendorResponse> vendors(String query, String categoryFilter, String cityFilter) {
        List<VendorResponse> marketplace = marketplaceVendors(query, categoryFilter, cityFilter);
        if (!marketplace.isEmpty()) {
            return marketplace;
        }

        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        List<User> users = userRepository.findByRole(Role.USER);

        return users.stream()
                .map(user -> toVendor(user, categories))
                .filter(v -> matchQuery(v, query))
                .filter(v -> matchCategory(v, categoryFilter))
                .filter(v -> cityFilter == null || cityFilter.isBlank() || v.city().equalsIgnoreCase(cityFilter.trim()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> marketplaceVendors(String query, String categoryFilter, String cityFilter) {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        List<Profile> profiles = profileRepository.findByIsVendorTrueAndVendorVerifiedTrue();

        return profiles.stream()
                .map(profile -> toMarketplaceVendor(profile, categories))
                .filter(v -> matchQuery(v, query))
                .filter(v -> matchCategory(v, categoryFilter))
                .filter(v -> cityFilter == null || cityFilter.isBlank() || v.city().equalsIgnoreCase(cityFilter.trim()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<VendorResponse> vendorById(Long id) {
        Optional<VendorResponse> marketplaceVendor = profileRepository.findByUserId(id)
                .filter(Profile::isVendor)
                .filter(Profile::isVendorVerified)
                .map(profile -> toMarketplaceVendor(profile, categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()));

        if (marketplaceVendor.isPresent()) {
            return marketplaceVendor;
        }

        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.USER)
                .map(u -> toVendor(u, categories));
    }

    @Transactional(readOnly = true)
    public List<CityResponse> cities() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        List<VendorResponse> vendors = userRepository.findByRole(Role.USER)
                .stream()
                .map(u -> toVendor(u, categories))
                .toList();

        Map<String, Long> grouped = vendors.stream()
                .collect(Collectors.groupingBy(VendorResponse::city, Collectors.counting()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new CityResponse(e.getKey(), "Sénégal", e.getValue(), cityEmoji(e.getKey())))
                .toList();
    }

    public List<ReviewResponse> reviews(Long vendorId) {
        Long base = vendorId == null ? 1L : vendorId;
        return List.of(
                new ReviewResponse(base * 10 + 1, base, "Aminata S.", 5, "Très satisfait du service.", LocalDate.now().minusDays(12).toString()),
                new ReviewResponse(base * 10 + 2, base, "Cheikh M.", 4, "Équipe réactive et professionnelle.", LocalDate.now().minusDays(5).toString())
        );
    }

    private VendorResponse toVendor(User user, List<Category> categories) {
        int idx = Math.floorMod(user.getId().intValue(), Math.max(categories.size(), 1));
        String categoryName = categories.isEmpty() ? "Services" : categories.get(idx).getName();
        String city = CITIES.get(Math.floorMod(user.getId().intValue(), CITIES.size()));
        double rating = Math.min(5.0, 4.2 + (Math.floorMod(user.getId().intValue(), 8) * 0.1));
        int reviews = 20 + Math.floorMod(user.getId().intValue(), 160);
        long views = 1000L + (user.getId() * 173);
        long leads = 50L + (user.getId() * 11);
        boolean verified = user.getId() % 3 != 0;
        String badge = rating >= 4.8 ? "Top" : (verified ? "Vérifié" : "Nouveau");
        String emoji = EMOJIS.get(Math.floorMod(user.getId().intValue(), EMOJIS.size()));

        return new VendorResponse(
                user.getId(),
                user.getFullName(),
                categoryName,
                city,
                "Sénégal",
                rating,
                reviews,
                verified,
                badge,
                emoji,
                "Prestataire professionnel disponible sur Raktakk.",
                List.of("Conseil", "Exécution", "Support"),
                "Sur devis",
                true,
                views,
                leads
        );
    }

            private VendorResponse toMarketplaceVendor(Profile profile, List<Category> categories) {
            User user = profile.getUser();
            List<ServiceOffer> offers = serviceOfferRepository.findByVendorProfileIdAndActiveTrueOrderByIdDesc(profile.getId());
            int idx = Math.floorMod(user.getId().intValue(), Math.max(categories.size(), 1));
            String categoryName = categories.isEmpty() ? "Services" : categories.get(idx).getName();
            String city = CITIES.get(Math.floorMod(user.getId().intValue(), CITIES.size()));
            double rating = profile.getRating() != null && profile.getRating() > 0
                ? profile.getRating()
                : Math.min(5.0, 4.3 + (Math.floorMod(user.getId().intValue(), 7) * 0.1));
            int reviews = profile.getTotalReviews() != null && profile.getTotalReviews() > 0
                ? profile.getTotalReviews()
                : 10 + (offers.size() * 8);
            String emoji = EMOJIS.get(Math.floorMod(user.getId().intValue(), EMOJIS.size()));

            List<String> serviceNames = offers.stream().map(ServiceOffer::getTitle).limit(3).toList();
            String price = offers.stream().findFirst()
                .map(s -> String.format(java.util.Locale.US, "%.0f FCFA", s.getPrice()))
                .orElse("Sur devis");

            return new VendorResponse(
                user.getId(),
                user.getFullName(),
                categoryName,
                city,
                "Sénégal",
                rating,
                reviews,
                profile.isVendorVerified(),
                profile.isVendorVerified() ? "Vérifié" : "Nouveau",
                emoji,
                profile.getDescription() == null || profile.getDescription().isBlank()
                    ? "Prestataire marketplace validé sur Raktakk."
                    : profile.getDescription(),
                serviceNames.isEmpty() ? List.of("Service marketplace") : serviceNames,
                price,
                true,
                1000L + user.getId() * 120,
                50L + user.getId() * 7
            );
            }

    private boolean matchQuery(VendorResponse vendor, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase().trim();
        String searchable = (vendor.name() + " " + vendor.category() + " " + String.join(" ", vendor.services())).toLowerCase();
        return searchable.contains(q);
    }

    private boolean matchCategory(VendorResponse vendor, String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isBlank()) {
            return true;
        }
        return vendor.category().toLowerCase().contains(categoryFilter.toLowerCase().trim());
    }

    private String cityEmoji(String city) {
        return switch (city) {
            case "Dakar" -> "🏙️";
            case "Abidjan" -> "🌆";
            case "Bamako" -> "🏛️";
            default -> "🌍";
        };
    }
}
