package com.raktakk.backend.repository;

import com.raktakk.backend.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);
    long countByActiveTrue();
}
