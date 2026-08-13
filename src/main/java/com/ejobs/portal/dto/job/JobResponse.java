package com.ejobs.portal.dto.job;

import com.ejobs.portal.model.EmploymentType;
import com.ejobs.portal.model.JobStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID employerId,
        String employerName,
        String title,
        String department,
        String location,
        EmploymentType employmentType,
        String salaryRange,
        String description,
        LocalDate deadline,
        JobStatus status,
        Instant createdAt
) {
}
