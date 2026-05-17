package com.raktakk.backend.repository;

import com.raktakk.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    java.util.Optional<Category> findBySlugIgnoreCase(String slug);
    long countByActiveTrue();
}
