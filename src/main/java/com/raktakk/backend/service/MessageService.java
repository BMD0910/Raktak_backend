package com.raktakk.backend.service;

import com.raktakk.backend.entity.Conversation;
import com.raktakk.backend.entity.Message;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.dto.MessageDTO;
import com.raktakk.backend.dto.SendMessageRequest;
import com.raktakk.backend.repository.ConversationRepository;
import com.raktakk.backend.repository.MessageRepository;
import com.raktakk.backend.repository.UserRepository;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long conversationId, String userEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify user is member of this conversation (client or vendor)
        if (!conversation.getClient().getId().equals(user.getId()) &&
            !conversation.getVendor().getId().equals(user.getId())) {
            throw new BadRequestException("Accès non autorisé à cette conversation");
        }

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(msg -> new MessageDTO(
                        msg.getId(),
                        msg.getSender().getId(),
                        msg.getSender().getFullName(),
                        msg.getContent(),
                        msg.getSentAt(),
                        msg.getSender().getId().equals(user.getId()) // isOwn based on sender
                ))
                .toList();
    }

    @Transactional
    public MessageDTO sendMessage(Long conversationId, String senderEmail, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        User sender = userRepository.findByEmailIgnoreCase(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Vérifier que l'utilisateur fait partie de la conversation
        if (!conversation.getClient().getId().equals(sender.getId()) &&
            !conversation.getVendor().getId().equals(sender.getId())) {
            throw new BadRequestException("Vous n'êtes pas autorisé à envoyer un message dans cette conversation");
        }

        // Vérifier que la commande est acceptée (conversation active)
        if (conversation.getOrder().getStatus().toString().equals("PENDING")) {
            throw new BadRequestException("La conversation n'est pas encore active");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.content())
                .build();

        Message saved = messageRepository.save(message);

        // Mettre à jour lastMessageAt de la conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return new MessageDTO(
                saved.getId(),
                saved.getSender().getId(),
                saved.getSender().getFullName(),
                saved.getContent(),
                saved.getSentAt(),
                true
        );
    }
}
