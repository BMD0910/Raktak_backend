package com.raktakk.backend.dto;

import com.raktakk.backend.entity.Role;

public record UserMeResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String accountStatus,
        String deactivationReason,
        String deactivationContact
) {
}
