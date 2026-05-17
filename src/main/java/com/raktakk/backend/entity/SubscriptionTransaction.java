package com.raktakk.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "subscription_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 40)
    private String planCode;

    @Column(nullable = false)
    private Long amountFcfa;

    @Column(nullable = false, length = 60)
    private String reference; // référence côté Unitech

    @Column(nullable = false, length = 40)
    private String status; // PENDING, PAID, FAILED

    @Column(length = 32)
    private String paymentMethod; // WAVE, ORANGE_QR, ORANGE_MAXIT, ORANGE_OM

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
