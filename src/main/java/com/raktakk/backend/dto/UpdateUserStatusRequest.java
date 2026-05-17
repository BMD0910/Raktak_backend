package com.raktakk.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequest {
    private Long id;
    private String accountStatus; // "active", "suspended", "inactive", "pending"
    private String reason;
    private Object contact; // may contain contact info
}
