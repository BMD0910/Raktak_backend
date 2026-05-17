package com.raktakk.backend.repository;

import com.raktakk.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByAdminId(Long adminId, Pageable pageable);
    Page<AuditLog> findByTargetType(String targetType, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    @Query("FROM AuditLog WHERE (:adminId IS NULL OR adminId = :adminId) " +
           "AND (:targetType IS NULL OR targetType = :targetType) " +
           "AND (:action IS NULL OR action = :action) " +
           "AND (:fromDate IS NULL OR createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR createdAt <= :toDate) " +
           "ORDER BY createdAt DESC")
    Page<AuditLog> findByFilters(
        @Param("adminId") Long adminId,
        @Param("targetType") String targetType,
        @Param("action") String action,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        Pageable pageable
    );

    List<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId);
}
