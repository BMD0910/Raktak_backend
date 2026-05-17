package com.raktakk.backend.repository;

import com.raktakk.backend.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findAllByOrderByDisplayOrderAsc();
    List<SubscriptionPlan> findByActiveTrueOrderByDisplayOrderAsc();
    Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);
}
