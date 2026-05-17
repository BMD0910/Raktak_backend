package com.raktakk.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationDTO(
    Long id,
    Long orderId,
    Long clientId,
    String clientName,
    Long vendorId,
    String vendorName,
    String serviceName,
    LocalDateTime createdAt,
    LocalDateTime lastMessageAt,
    List<MessageDTO> messages
) {}
