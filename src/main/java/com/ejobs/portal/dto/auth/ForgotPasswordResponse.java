package com.ejobs.portal.dto.auth;

/**
 * @param message    always identical, whether or not the email is registered
 * @param resetToken populated only under the "local" profile, and only when a matching
 *                   account was found. Null everywhere else - echoing it back in a real
 *                   environment would hand the reset to whoever made the request.
 */
public record ForgotPasswordResponse(
        String message,
        String resetToken
) {
}
