package com.raktakk.backend.controller;

import com.raktakk.backend.dto.ConversationDTO;
import com.raktakk.backend.service.ConversationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationServiceImpl conversationService;

    /**
     * Get a conversation (with all messages) for an order
     * Only accessible to client or vendor of the order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ConversationDTO> getConversation(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        ConversationDTO conversation = conversationService.getConversation(orderId, userEmail);
        return ResponseEntity.ok(conversation);
    }
}
