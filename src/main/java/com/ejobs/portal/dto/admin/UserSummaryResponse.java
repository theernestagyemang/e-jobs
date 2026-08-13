package com.ejobs.portal.dto.admin;

import com.ejobs.portal.model.Role;

import java.time.Instant;
import java.util.UUID;

/** Admin user list row. Deliberately omits passwordHash. */
public record UserSummaryResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {
}
