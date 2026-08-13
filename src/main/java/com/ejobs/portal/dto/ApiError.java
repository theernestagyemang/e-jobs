package com.ejobs.portal.dto;

import java.time.Instant;

/**
 * Consistent error envelope for all non-2xx responses.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
