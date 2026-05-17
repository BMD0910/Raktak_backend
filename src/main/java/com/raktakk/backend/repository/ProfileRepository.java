package com.raktakk.backend.repository;

import com.raktakk.backend.entity.Profile;
import com.raktakk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser(User user);
    Optional<Profile> findByUserId(Long userId);
    List<Profile> findByIsVendorTrue();
    List<Profile> findByIsVendorTrueAndVendorVerifiedTrue();
    long countByIsVendorTrue();
    long countByIsVendorFalse();
}
