package com.raktakk.backend.repository;

import com.raktakk.backend.entity.User;
import com.raktakk.backend.entity.AccountStatus;
import com.raktakk.backend.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRole(Role role);
    long countByAccountStatus(AccountStatus accountStatus);
    long countByEnabledTrue();
    long countByCreatedAtAfter(java.time.Instant after);
    long countByCreatedAtBetween(java.time.Instant start, java.time.Instant end);
}
