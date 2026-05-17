package com.raktakk.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
    @NotBlank(message = "content est requis")
    String content
) {}
