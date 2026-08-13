package com.ejobs.portal.service;

import com.ejobs.portal.config.security.JwtService;
import com.ejobs.portal.dto.auth.AuthResponse;
import com.ejobs.portal.dto.auth.ForgotPasswordRequest;
import com.ejobs.portal.dto.auth.ForgotPasswordResponse;
import com.ejobs.portal.dto.auth.LoginRequest;
import com.ejobs.portal.dto.auth.RegisterRequest;
import com.ejobs.portal.dto.auth.ResetPasswordRequest;
import com.ejobs.portal.exception.InvalidOrExpiredTokenException;
import com.ejobs.portal.model.AuditEventType;
import com.ejobs.portal.model.AuditLog;
import com.ejobs.portal.model.PasswordResetToken;
import com.ejobs.portal.model.Role;
import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.AuditLogRepository;
import com.ejobs.portal.repository.PasswordResetTokenRepository;
import com.ejobs.portal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * FR-AUTH-01 registration, FR-AUTH-03 login. Both paths end with a signed JWT
 * (FR-AUTH-02) so the client is authenticated immediately after either call.
 *
 * <p>Every attempt is written to the audit trail for the System Administrator.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private static final String GENERIC_RESET_MESSAGE =
            "If an account exists for that email address, a password reset link has been sent.";
    private static final String GENERIC_TOKEN_MESSAGE =
            "This password reset link is invalid or has expired. Please request a new one.";

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean localProfileActive;

    public AuthService(UserRepository userRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       Environment environment) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.localProfileActive = List.of(environment.getActiveProfiles()).contains("local");
    }

    /** FR-AUTH-01: reject a taken email, BCrypt the password (NFR-SEC-01), issue a token. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Privilege escalation guard: role arrives in the request body, so without this
        // anyone could mint themselves an administrator account. A hard rule, not a UI
        // restriction - the only two routes to an ADMIN account are AdminBootstrapRunner
        // at startup and AdminService.createAdmin called by an existing admin.
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Administrator accounts cannot be self-registered");
        }

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();

        User saved;
        try {
            // Flush inside the try: two concurrent registrations can both clear the
            // existsByEmail check above, and only uk_users_email actually stops the second.
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        recordAudit(AuditEventType.REGISTER, saved.getEmail(), "Registered as " + saved.getRole());

        return toAuthResponse(saved);
    }

    /**
     * FR-AUTH-03. An unknown email and a wrong password raise the same exception with the
     * same message, so the endpoint cannot be used to discover which emails are registered.
     *
     * <p>Intentionally not @Transactional: a LOGIN_FAILURE row must survive the exception
     * thrown immediately after it. Inside a transaction that write would be rolled back,
     * and the audit trail would only ever show successful logins.
     */
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()
                || !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
            recordAudit(AuditEventType.LOGIN_FAILURE, email, "Invalid email or password");
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = found.get();

        if (!user.isActive()) {
            recordAudit(AuditEventType.LOGIN_FAILURE, email, "Account is deactivated");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account has been deactivated");
        }

        recordAudit(AuditEventType.LOGIN_SUCCESS, email, "Signed in as " + user.getRole());

        return toAuthResponse(user);
    }

    /**
     * Starts a password reset.
     *
     * <p>DELIBERATE: the response is byte-identical - same 200, same message - whether or
     * not the email is registered. Any difference in body or status would turn this
     * public endpoint into a user-enumeration oracle, letting anyone test which addresses
     * hold accounts.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> found = userRepository.findByEmail(email);

        if (found.isEmpty()) {
            return new ForgotPasswordResponse(GENERIC_RESET_MESSAGE, null);
        }

        User user = found.get();
        String token = UUID.randomUUID().toString();

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plus(TOKEN_TTL))
                .used(false)
                .build());

        // Stands in for the mail transport this project does not have.
        log.info("[SIMULATED EMAIL] Password reset link for {}: token={}", user.getEmail(), token);

        recordAudit(AuditEventType.PASSWORD_RESET_REQUESTED, user.getEmail(),
                "Password reset requested");

        // Echoed back only on a developer machine, so the flow can be exercised without
        // a mailbox. Returning this anywhere else would hand the reset to the caller.
        return new ForgotPasswordResponse(GENERIC_RESET_MESSAGE, localProfileActive ? token : null);
    }

    /**
     * Redeems a reset token. Unknown, already-used and expired tokens are indistinguishable
     * in the response, for the same reason as above.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidOrExpiredTokenException(GENERIC_TOKEN_MESSAGE));

        if (!resetToken.isRedeemable(Instant.now())) {
            throw new InvalidOrExpiredTokenException(GENERIC_TOKEN_MESSAGE);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Single use: burn it so the same link cannot be replayed.
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        recordAudit(AuditEventType.PASSWORD_RESET_COMPLETED, user.getEmail(),
                "Password reset completed");
    }

    private void recordAudit(AuditEventType eventType, String actorEmail, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .eventType(eventType)
                .actorEmail(actorEmail)
                .detail(detail)
                .build());
    }

    /**
     * Emails are compared case-insensitively. PostgreSQL unique constraints are
     * case-sensitive, so without this "Ada@x.com" and "ada@x.com" would be two accounts.
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getEmail(), user.getRole(), user.getFullName());
    }
}
