package com.raktakk.backend.repository;

import com.raktakk.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByOrderId(Long orderId);
    List<Conversation> findByClientIdOrderByLastMessageAtDesc(Long clientId);
    List<Conversation> findByVendorIdOrderByLastMessageAtDesc(Long vendorId);
    Optional<Conversation> findByIdAndClientId(Long conversationId, Long clientId);
    Optional<Conversation> findByIdAndVendorId(Long conversationId, Long vendorId);
}
