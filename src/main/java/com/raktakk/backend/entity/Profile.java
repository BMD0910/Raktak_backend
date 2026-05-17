package com.raktakk.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private boolean isVendor;

    @Column(nullable = false)
    private boolean vendorVerified;

    @Column(nullable = false)
    private Boolean subscriptionActive;

    @Column(nullable = false)
    private Boolean profileCompleted;

    @Column(length = 64)
    private String subscriptionPlanCode;

    @Column(length = 120)
    private String profession;

    @Column(length = 2000)
    private String skills;

    @Column(length = 2000)
    private String experience;

    @Column(length = 1000)
    private String description;

    @Column(length = 64)
    private String phone;

    @Column(length = 120)
    private String location;

    @Column(length = 512)
    private String portfolioUrl;

    @Column(length = 1000)
    private String socialLinks;

    @Column(length = 512)
    private String avatar;

    @Column(nullable = false)
    private Double rating;

    @Column(nullable = false)
    private Integer totalReviews;

    @PrePersist
    public void onCreate() {
        if (rating == null) rating = 0.0;
        if (totalReviews == null) totalReviews = 0;
        if (subscriptionActive == null) subscriptionActive = false;
        if (profileCompleted == null) profileCompleted = false;
    }
}
