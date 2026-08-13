package com.ejobs.portal.dto.job;

import com.ejobs.portal.model.EmploymentType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobCreateRequest(
        @NotBlank String title,
        String department,
        @NotBlank String location,
        @NotNull EmploymentType employmentType,
        String salaryRange,
        @NotBlank String description,
        @NotNull @FutureOrPresent LocalDate deadline
) {
}
