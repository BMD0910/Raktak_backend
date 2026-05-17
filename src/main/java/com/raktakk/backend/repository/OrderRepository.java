package com.raktakk.backend.repository;

import com.raktakk.backend.entity.Order;
import com.raktakk.backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<Order> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    List<Order> findByClientIdAndStatusOrderByCreatedAtDesc(Long clientId, OrderStatus status);
    List<Order> findByVendorIdAndStatusOrderByCreatedAtDesc(Long vendorId, OrderStatus status);
    Optional<Order> findByIdAndClientId(Long id, Long clientId);
    Optional<Order> findByIdAndVendorId(Long id, Long vendorId);
    long countByStatus(OrderStatus status);
}

