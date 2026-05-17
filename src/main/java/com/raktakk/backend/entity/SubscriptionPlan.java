package com.raktakk.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Long priceFcfa;

    @Column(nullable = false, length = 600)
    private String description;

    @Column(nullable = false, length = 3000)
    private String featuresText;

    @Column(nullable = false)
    private Integer maxServices;

    @Column(nullable = false)
    private Integer maxFeaturedServices;

    @Column(nullable = false)
    private boolean allowFeatured;

    @Column(nullable = false)
    private boolean allowPremiumBadge;

    @Column(nullable = false)
    private boolean requireCompleteProfile;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (displayOrder == null) displayOrder = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
        if (displayOrder == null) displayOrder = 0;
    }
}
