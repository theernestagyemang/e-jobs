package com.ejobs.portal.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * No role field on purpose - this endpoint only ever mints ADMIN accounts, and the
 * caller must already be an authenticated admin.
 */
public record CreateAdminRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
