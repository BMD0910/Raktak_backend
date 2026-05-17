package com.raktakk.backend.repository;

import com.raktakk.backend.entity.SubscriptionTransaction;
import com.raktakk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionTransactionRepository extends JpaRepository<SubscriptionTransaction, Long> {
    Optional<SubscriptionTransaction> findByReference(String reference);
    List<SubscriptionTransaction> findByUserOrderByCreatedAtDesc(User user);
}
