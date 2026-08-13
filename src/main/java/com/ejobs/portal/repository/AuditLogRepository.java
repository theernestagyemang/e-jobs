package com.ejobs.portal.repository;

import com.ejobs.portal.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Admin audit view - newest first, backed by idx_audit_logs_created_at. */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Retention sweep. Also uses idx_audit_logs_created_at. Returns rows removed. */
    long deleteByCreatedAtBefore(Instant cutoff);
}
