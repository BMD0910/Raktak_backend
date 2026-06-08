package com.raktakk.backend.repository;

import com.raktakk.backend.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);
    List<Subcategory> findByCategoryIdOrderByNameAsc(Long categoryId);
    java.util.Optional<Subcategory> findByCategoryIdAndSlugIgnoreCase(Long categoryId, String slug);
    long countByActiveTrue();
    long countByCategoryId(Long categoryId);
}
