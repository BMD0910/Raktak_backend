package com.raktakk.backend.dto;

import com.raktakk.backend.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderDTO(
    Long id,
    Long clientId,
    String clientName,
    Long vendorId,
    String vendorName,
    Long serviceId,
    String serviceName,
    Double servicePrice,
    OrderStatus status,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
