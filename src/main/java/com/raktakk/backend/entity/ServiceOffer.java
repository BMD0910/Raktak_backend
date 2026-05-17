package com.raktakk.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_profile_id", nullable = false)
    private Profile vendorProfile;

    @Column(nullable = false, length = 120)
    private String category;

    @Column(length = 512)
    private String imageUrl;

    @Column(nullable = false)
    private Integer deliveryTime;

    @Column(nullable = false)
    private Boolean featured;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceStatus status;

    @Column(length = 500)
    private String deactivationReason;

    @Column
    private Instant deactivatedAt;

    @Column
    private Long deactivatedBy;

    @PrePersist
    public void onCreate() {
        if (featured == null) featured = false;
        if (status == null) status = ServiceStatus.ACTIVE;
    }
}
