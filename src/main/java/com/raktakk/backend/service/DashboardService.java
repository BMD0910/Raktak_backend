package com.raktakk.backend.service;

import com.raktakk.backend.dto.DashboardSummaryResponse;
import com.raktakk.backend.repository.CategoryRepository;
import com.raktakk.backend.repository.SubcategoryRepository;
import com.raktakk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public DashboardSummaryResponse summary(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        return new DashboardSummaryResponse(
                userRepository.count(),
                userRepository.countByRole(com.raktakk.backend.entity.Role.USER),
                categoryRepository.countByActiveTrue(),
                subcategoryRepository.countByActiveTrue(),
                role
        );
    }
}
