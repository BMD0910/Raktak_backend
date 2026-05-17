package com.raktakk.backend.service;

import com.raktakk.backend.dto.ContactMessageRequest;
import com.raktakk.backend.dto.ContactMessageResponse;
import com.raktakk.backend.entity.ContactMessage;
import com.raktakk.backend.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ContactMessageService {
    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;

    public ContactMessageResponse submitContactMessage(ContactMessageRequest request) {
        log.info("📨 Traitement du message de contact de: {}", request.getEmail());

        // Créer l'entité
        ContactMessage contactMessage = ContactMessage.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .build();

        // Sauvegarder en base de données
        ContactMessage saved = contactMessageRepository.save(contactMessage);
        log.info("✅ Message sauvegardé avec ID: {}", saved.getId());

        // Envoyer les emails
        try {
            emailService.sendContactNotificationToAdmin(
                    request.getFirstName() + " " + request.getLastName(),
                    request.getEmail(),
                    request.getSubject(),
                    request.getMessage(),
                    request.getPhone()
            );
            log.info("✅ Email de notification envoyé à l'admin");

            emailService.sendConfirmationToVisitor(request.getFirstName(), request.getEmail());
            log.info("✅ Email de confirmation envoyé au visiteur");
        } catch (Exception e) {
            log.error("⚠️ Erreur lors de l'envoi des emails: {}", e.getMessage());
            // Les emails ne sont pas bloquants - le message est déjà sauvegardé
        }

        return toResponse(saved);
    }

    public Page<ContactMessageResponse> getUnreadMessages(int page, int size) {
        Page<ContactMessage> messages = contactMessageRepository.findByIsReadFalse(
                PageRequest.of(page, size)
        );
        return messages.map(this::toResponse);
    }

    public Page<ContactMessageResponse> getAllMessages(int page, int size) {
        Page<ContactMessage> messages = contactMessageRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );
        return messages.map(this::toResponse);
    }

    public ContactMessageResponse getMessageById(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        // Marquer comme lu
        if (!message.getIsRead()) {
            message.setIsRead(true);
            contactMessageRepository.save(message);
        }

        return toResponse(message);
    }

    public void deleteMessage(Long id) {
        contactMessageRepository.deleteById(id);
        log.info("✅ Message {} supprimé", id);
    }

    private ContactMessageResponse toResponse(ContactMessage entity) {
        return ContactMessageResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .subject(entity.getSubject())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .isRead(entity.getIsRead())
                .adminReply(entity.getAdminReply())
                .repliedAt(entity.getRepliedAt())
                .build();
    }
}
