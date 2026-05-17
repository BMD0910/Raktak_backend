package com.raktakk.backend.repository;

import com.raktakk.backend.entity.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {
	Optional<SiteSettings> findFirstByOrderByUpdatedAtDesc();
}
