package com.ejobs.portal.dto.admin;

import com.ejobs.portal.model.AuditEventType;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditEventType eventType,
        String actorEmail,
        String detail,
        Instant createdAt
) {
}
