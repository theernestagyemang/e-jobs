package com.ejobs.portal.dto.application;

import com.ejobs.portal.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusUpdateRequest(
        @NotNull ApplicationStatus status
) {
}
