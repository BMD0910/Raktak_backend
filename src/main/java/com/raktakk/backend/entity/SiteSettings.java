package com.raktakk.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "site_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String siteName;

    @Column(nullable = false, length = 120)
    private String supportEmail;

    @Column(length = 50)
    private String supportPhone;

    @Column(nullable = false)
    private boolean maintenanceMode;

    @Column(length = 1000)
    private String maintenanceMessage;

    @Column(nullable = false)
    private Integer auditRetentionDays;

    @Column(nullable = false)
    private Boolean allowNewRegistrations;

    @Column(nullable = false)
    private Boolean allowNewVendorApplications;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (siteName == null || siteName.isBlank()) siteName = "Raktakk";
        if (supportEmail == null || supportEmail.isBlank()) supportEmail = "support@raktakk.com";
        if (auditRetentionDays == null) auditRetentionDays = 90;
        if (allowNewRegistrations == null) allowNewRegistrations = true;
        if (allowNewVendorApplications == null) allowNewVendorApplications = true;
        if (maintenanceMessage == null) maintenanceMessage = "";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
        if (auditRetentionDays == null) auditRetentionDays = 90;
        if (allowNewRegistrations == null) allowNewRegistrations = true;
        if (allowNewVendorApplications == null) allowNewVendorApplications = true;
        if (maintenanceMessage == null) maintenanceMessage = "";
    }
}
