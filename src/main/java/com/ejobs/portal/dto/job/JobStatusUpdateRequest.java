package com.ejobs.portal.dto.job;

import com.ejobs.portal.model.JobStatus;
import jakarta.validation.constraints.NotNull;

public record JobStatusUpdateRequest(
        @NotNull JobStatus status
) {
}
