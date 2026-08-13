package com.ejobs.portal.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Boxed Boolean rather than primitive: a primitive would default to false when the
 * field is missing from the payload, silently deactivating instead of failing @NotNull.
 */
public record UserStatusUpdateRequest(
        @NotNull Boolean active
) {
}
