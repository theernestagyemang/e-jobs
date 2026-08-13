package com.ejobs.portal.dto.application;

import com.ejobs.portal.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID applicantId,
        String applicantName,
        String coverNote,
        String resumeUrl,
        ApplicationStatus status,
        Instant appliedAt
) {
}
