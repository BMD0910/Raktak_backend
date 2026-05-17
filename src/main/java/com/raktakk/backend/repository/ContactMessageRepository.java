package com.raktakk.backend.repository;

import com.raktakk.backend.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    Page<ContactMessage> findByIsReadFalse(Pageable pageable);
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
