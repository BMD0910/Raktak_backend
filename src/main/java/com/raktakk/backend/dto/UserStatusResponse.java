package com.raktakk.backend.dto;

import com.raktakk.backend.entity.Role;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusResponse {
    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private String accountStatus;
    private String deactivationReason;
    private String deactivationContact;
    private Instant deactivatedAt;
    private Long deactivatedBy;
}
