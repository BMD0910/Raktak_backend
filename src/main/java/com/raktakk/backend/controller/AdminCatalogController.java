package com.raktakk.backend.controller;

import com.raktakk.backend.entity.Category;
import com.raktakk.backend.entity.Subcategory;
import com.raktakk.backend.repository.CategoryRepository;
import com.raktakk.backend.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\p{IsLatin}\\p{Nd}]+");

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @GetMapping("/categories.php")
    @Transactional(readOnly = true)
    public Map<String, Object> categories() {
        List<Map<String, Object>> data = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder", "name")).stream()
                .map(this::mapCategory)
                .toList();
        return Map.of("ok", true, "data", data);
    }

    @PostMapping("/categories-save.php")
    @Transactional
    public Map<String, Object> saveCategory(@RequestBody Map<String, Object> body) {
        Long id = parseLong(body.get("id"));
        String name = trim(body.get("name"));
        String slug = trim(body.get("slug"));
        String icon = defaultString(trim(body.get("icon")), "folder");
        Integer displayOrder = parseInt(body.get("displayOrder"), 0);
        boolean active = parseBoolean(body.get("active"), true);

        if (name == null || name.isBlank()) {
            return fail("Le nom de la catégorie est requis");
        }

        if (slug == null || slug.isBlank()) {
            slug = slugify(name);
        } else {
            slug = slugify(slug);
        }

        Optional<Category> duplicate = categoryRepository.findBySlugIgnoreCase(slug);
        if (duplicate.isPresent() && (id == null || !Objects.equals(duplicate.get().getId(), id))) {
            return fail("Une catégorie avec ce slug existe déjà");
        }

        Category category = id == null
                ? Category.builder().build()
                : categoryRepository.findById(id).orElseThrow();

        category.setName(name);
        category.setSlug(slug);
        category.setIcon(icon);
        category.setDisplayOrder(displayOrder);
        category.setActive(active);

        Category saved = categoryRepository.save(category);
        return ok(mapCategory(saved));
    }

    @PostMapping("/categories-toggle.php")
    @Transactional
    public Map<String, Object> toggleCategory(@RequestBody Map<String, Object> body) {
        Long id = parseLong(body.get("id"));
        if (id == null) return fail("Paramètre id requis");

        Category category = categoryRepository.findById(id).orElseThrow();
        if (body.containsKey("active")) {
            category.setActive(parseBoolean(body.get("active"), !category.isActive()));
        } else {
            category.setActive(!category.isActive());
        }

        return ok(mapCategory(categoryRepository.save(category)));
    }

    @GetMapping("/subcategories.php")
    @Transactional(readOnly = true)
    public Map<String, Object> subcategories(@RequestParam(required = false) Long categoryId) {
        List<Map<String, Object>> data = subcategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(subcategory -> categoryId == null || Objects.equals(subcategory.getCategory().getId(), categoryId))
                .map(this::mapSubcategory)
                .toList();
        return Map.of("ok", true, "data", data);
    }

    @PostMapping("/subcategories-save.php")
    @Transactional
    public Map<String, Object> saveSubcategory(@RequestBody Map<String, Object> body) {
        Long id = parseLong(body.get("id"));
        Long categoryId = parseLong(body.get("categoryId"));
        String name = trim(body.get("name"));
        String slug = trim(body.get("slug"));
        boolean active = parseBoolean(body.get("active"), true);

        if (categoryId == null) return fail("La catégorie parente est requise");
        if (name == null || name.isBlank()) return fail("Le nom de la sous-catégorie est requis");

        Category category = categoryRepository.findById(categoryId).orElseThrow();
        if (slug == null || slug.isBlank()) {
            slug = slugify(name);
        } else {
            slug = slugify(slug);
        }

        Optional<Subcategory> duplicate = subcategoryRepository.findByCategoryIdAndSlugIgnoreCase(categoryId, slug);
        if (duplicate.isPresent() && (id == null || !Objects.equals(duplicate.get().getId(), id))) {
            return fail("Une sous-catégorie avec ce slug existe déjà dans cette catégorie");
        }

        Subcategory subcategory = id == null
                ? Subcategory.builder().build()
                : subcategoryRepository.findById(id).orElseThrow();

        subcategory.setCategory(category);
        subcategory.setName(name);
        subcategory.setSlug(slug);
        subcategory.setActive(active);

        Subcategory saved = subcategoryRepository.save(subcategory);
        return ok(mapSubcategory(saved));
    }

    @PostMapping("/subcategories-toggle.php")
    @Transactional
    public Map<String, Object> toggleSubcategory(@RequestBody Map<String, Object> body) {
        Long id = parseLong(body.get("id"));
        if (id == null) return fail("Paramètre id requis");

        Subcategory subcategory = subcategoryRepository.findById(id).orElseThrow();
        if (body.containsKey("active")) {
            subcategory.setActive(parseBoolean(body.get("active"), !subcategory.isActive()));
        } else {
            subcategory.setActive(!subcategory.isActive());
        }

        return ok(mapSubcategory(subcategoryRepository.save(subcategory)));
    }

    private Map<String, Object> mapCategory(Category category) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", category.getId());
        map.put("name", category.getName());
        map.put("slug", category.getSlug());
        map.put("icon", category.getIcon());
        map.put("displayOrder", category.getDisplayOrder());
        map.put("active", category.isActive());
        map.put("subcategoriesCount", subcategoryRepository.countByCategoryId(category.getId()));
        return map;
    }

    private Map<String, Object> mapSubcategory(Subcategory subcategory) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", subcategory.getId());
        map.put("categoryId", subcategory.getCategory() == null ? null : subcategory.getCategory().getId());
        map.put("categoryName", subcategory.getCategory() == null ? null : subcategory.getCategory().getName());
        map.put("name", subcategory.getName());
        map.put("slug", subcategory.getSlug());
        map.put("active", subcategory.isActive());
        return map;
    }

    private Map<String, Object> ok(Object data) {
        return Map.of("ok", true, "data", data);
    }

    private Map<String, Object> fail(String message) {
        return Map.of("ok", false, "message", message);
    }

    private static String trim(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Long parseLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer parseInt(Object value, Integer fallback) {
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean parseBoolean(Object value, boolean fallback) {
        if (value == null) return fallback;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return NON_LATIN.matcher(normalized.toLowerCase())
                .replaceAll("-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
    }
}