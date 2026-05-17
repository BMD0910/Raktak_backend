package com.raktakk.backend.entity;

public enum OrderStatus {
    PENDING,      // Demande initiale
    ACCEPTED,     // Prestataire a accepté
    REJECTED,     // Prestataire a refusé
    COMPLETED,    // Travail terminé
    CANCELLED     // Annulée par client
}
