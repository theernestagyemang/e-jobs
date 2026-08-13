package com.ejobs.portal.service;

import com.ejobs.portal.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Keeps the audit trail bounded.
 *
 * <p>audit_logs is append-only and every login attempt writes a row, so a burst of failed
 * logins would otherwise grow the table without limit. This trims anything past the
 * retention window on a nightly schedule.
 *
 * <p>Set {@code audit.retention-days} to 0 or less to keep entries forever.
 */
@Component
public class AuditLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionJob.class);

    private final AuditLogRepository auditLogRepository;
    private final int retentionDays;

    public AuditLogRetentionJob(AuditLogRepository auditLogRepository,
                                @Value("${audit.retention-days}") int retentionDays) {
        this.auditLogRepository = auditLogRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${audit.retention-cron}")
    @Transactional
    public void purgeExpiredEntries() {
        if (retentionDays <= 0) {
            return;
        }

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long removed = auditLogRepository.deleteByCreatedAtBefore(cutoff);

        if (removed > 0) {
            log.info("Audit retention: removed {} entries older than {} ({} days)",
                    removed, cutoff, retentionDays);
        }
    }
}
