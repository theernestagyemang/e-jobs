package com.ejobs.portal.dto.auth;

import com.ejobs.portal.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        // BCrypt silently ignores input past 72 bytes, so anything longer is not fully
        // hashed. An explicit max also keeps the violation message readable instead of
        // rendering the default Integer.MAX_VALUE upper bound.
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role
) {
}
