package com.ejobs.portal.dto.job;

import com.ejobs.portal.model.EmploymentType;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

/**
 * Partial update - every field is optional. A null field means "leave unchanged",
 * so no @NotBlank/@NotNull here.
 * <p>
 * {@code @FutureOrPresent} is retained because that constraint passes on null, so it
 * stays optional while still rejecting a deadline moved into the past.
 */
public record JobUpdateRequest(
        String title,
        String department,
        String location,
        EmploymentType employmentType,
        String salaryRange,
        String description,
        @FutureOrPresent LocalDate deadline
) {
}
