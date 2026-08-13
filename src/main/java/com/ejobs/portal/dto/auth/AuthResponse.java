package com.ejobs.portal.dto.auth;

import com.ejobs.portal.model.Role;

public record AuthResponse(
        String token,
        String email,
        Role role,
        String fullName
) {
}
