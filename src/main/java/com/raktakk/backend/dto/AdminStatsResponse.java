package com.raktakk.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {
    private long vendorsCount;
    private long clientsCount;
    private long disabledCount;
    private long servicesCount;
    private long suspendedServicesCount;
}
