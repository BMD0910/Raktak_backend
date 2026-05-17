package com.raktakk.backend.controller;

import com.raktakk.backend.dto.ContactMessageRequest;
import com.raktakk.backend.dto.ContactMessageResponse;
import com.raktakk.backend.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ContactController {
    private final ContactMessageService contactMessageService;

    /**
     * POST /api/contact/submit - Soumettre un message de contact
     * Accessible par TOUS (pas d'authentification requise)
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitContactMessage(@Valid @RequestBody ContactMessageRequest request) {
        try {
            log.info("📨 Nouvelle demande de contact reçue: {}", request.getEmail());
            ContactMessageResponse response = contactMessageService.submitContactMessage(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse(
                    true,
                    "Message envoyé avec succès. Nous vous répondrons sous 24h.",
                    response
            ));
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du message de contact: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(
                    false,
                    "Erreur lors de l'envoi du message: " + e.getMessage(),
                    null
            ));
        }
    }

    /**
     * GET /api/contact/admin/unread - Récupérer les messages non lus (ADMIN ONLY)
     */
    @GetMapping("/admin/unread")
    public ResponseEntity<?> getUnreadMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ContactMessageResponse> messages = contactMessageService.getUnreadMessages(page, size);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Messages non lus récupérés",
                    messages
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(
                    false,
                    "Erreur: " + e.getMessage(),
                    null
            ));
        }
    }

    /**
     * GET /api/contact/admin/all - Récupérer tous les messages (ADMIN ONLY)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ContactMessageResponse> messages = contactMessageService.getAllMessages(page, size);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Tous les messages récupérés",
                    messages
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(
                    false,
                    "Erreur: " + e.getMessage(),
                    null
            ));
        }
    }

    /**
     * GET /api/contact/admin/{id} - Récupérer un message par ID (ADMIN ONLY)
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getMessageById(@PathVariable Long id) {
        try {
            ContactMessageResponse message = contactMessageService.getMessageById(id);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Message récupéré",
                    message
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            ));
        }
    }

    /**
     * DELETE /api/contact/admin/{id} - Supprimer un message (ADMIN ONLY)
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        try {
            contactMessageService.deleteMessage(id);
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Message supprimé avec succès",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(
                    false,
                    "Erreur: " + e.getMessage(),
                    null
            ));
        }
    }

    /**
     * Classe wrapper pour les réponses API
     */
    public static class ApiResponse {
        public boolean success;
        public String message;
        public Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
}
