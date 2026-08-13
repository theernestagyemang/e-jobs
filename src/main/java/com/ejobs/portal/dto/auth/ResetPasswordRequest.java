package com.ejobs.portal.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        // Upper bound matches RegisterRequest: BCrypt ignores input past 72 bytes.
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
