package com.raktakk.backend.dto;

import com.raktakk.backend.entity.AuditLog;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private String action;
    private String targetType;
    private Long targetId;
    private String reason;
    private String extra;
    private Instant createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return builder()
            .id(log.getId())
            .adminId(log.getAdminId())
            .action(log.getAction())
            .targetType(log.getTargetType())
            .targetId(log.getTargetId())
            .reason(log.getReason())
            .extra(log.getExtra())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
