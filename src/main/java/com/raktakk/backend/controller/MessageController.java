package com.raktakk.backend.controller;

import com.raktakk.backend.dto.MessageDTO;
import com.raktakk.backend.dto.SendMessageRequest;
import com.raktakk.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    /**
     * Get all messages for a conversation
     * Only accessible to participants (client or vendor)
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        List<MessageDTO> messages = messageService.getMessages(conversationId, userEmail);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message in a conversation
     * Only participants can send messages
     * Order must be ACCEPTED (not in PENDING state)
     */
    @PostMapping("/{conversationId}")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        MessageDTO messageDTO = messageService.sendMessage(conversationId, userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageDTO);
    }
}
