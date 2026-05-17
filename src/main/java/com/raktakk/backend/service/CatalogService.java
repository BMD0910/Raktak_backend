package com.raktakk.backend.service;

import com.raktakk.backend.dto.CategoryResponse;
import com.raktakk.backend.dto.SubcategoryResponse;
import com.raktakk.backend.repository.CategoryRepository;
import com.raktakk.backend.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public List<CategoryResponse> categories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getIcon(), c.getDisplayOrder()))
                .toList();
    }

    public List<SubcategoryResponse> subcategories(Long categoryId) {
        return subcategoryRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(categoryId).stream()
                .map(s -> new SubcategoryResponse(s.getId(), s.getCategory().getId(), s.getName(), s.getSlug()))
                .toList();
    }
}
