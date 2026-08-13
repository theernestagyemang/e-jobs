package com.ejobs.portal.dto.job;

import com.ejobs.portal.model.EmploymentType;
import com.ejobs.portal.model.JobStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lighter projection for list and search results (FR-JOB-04) - omits the full
 * description so result pages stay small.
 */
public record JobSummaryResponse(
        UUID id,
        UUID employerId,
        String employerName,
        String title,
        String department,
        String location,
        EmploymentType employmentType,
        String salaryRange,
        LocalDate deadline,
        JobStatus status,
        Instant createdAt
) {
}
