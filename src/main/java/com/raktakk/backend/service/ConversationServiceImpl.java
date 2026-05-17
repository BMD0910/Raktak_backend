package com.raktakk.backend.service;

import com.raktakk.backend.entity.Conversation;
import com.raktakk.backend.entity.Order;
import com.raktakk.backend.dto.ConversationDTO;
import com.raktakk.backend.dto.MessageDTO;
import com.raktakk.backend.repository.ConversationRepository;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl {

    private final ConversationRepository conversationRepository;
    private final MessageService messageService;

    @Transactional
    public Conversation createConversation(Order order) {
        // Vérifier si une conversation existe déjà
        if (conversationRepository.findByOrderId(order.getId()).isPresent()) {
            throw new BadRequestException("Une conversation existe déjà pour cette commande");
        }

        Conversation conversation = Conversation.builder()
                .order(order)
                .client(order.getClient())
                .vendor(order.getVendor())
                .build();

        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public ConversationDTO getConversation(Long orderId, String userEmail) {
        Conversation conversation = conversationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("Conversation non trouvée pour cette commande"));

        // Vérifier que l'utilisateur fait partie de la conversation
        if (!conversation.getClient().getEmail().equalsIgnoreCase(userEmail) &&
            !conversation.getVendor().getEmail().equalsIgnoreCase(userEmail)) {
            throw new BadRequestException("Accès non autorisé à cette conversation");
        }

        var messages = messageService.getMessages(conversation.getId(), userEmail);

        return new ConversationDTO(
                conversation.getId(),
                conversation.getOrder().getId(),
                conversation.getClient().getId(),
                conversation.getClient().getFullName(),
                conversation.getVendor().getId(),
                conversation.getVendor().getFullName(),
                conversation.getOrder().getService().getTitle(),
                conversation.getCreatedAt(),
                conversation.getLastMessageAt(),
                messages
        );
    }

    @Transactional(readOnly = true)
    public boolean belongsToConversation(Long conversationId, String userEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null) return false;

        return conversation.getClient().getEmail().equalsIgnoreCase(userEmail) ||
               conversation.getVendor().getEmail().equalsIgnoreCase(userEmail);
    }
}
