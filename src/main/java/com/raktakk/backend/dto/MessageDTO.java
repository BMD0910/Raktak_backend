package com.raktakk.backend.dto;

import java.time.LocalDateTime;

public record MessageDTO(
    Long id,
    Long senderId,
    String senderName,
    String content,
    LocalDateTime sentAt,
    boolean isOwn
) {}
