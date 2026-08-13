package com.ejobs.portal.config;

import com.ejobs.portal.model.AuditEventType;
import com.ejobs.portal.model.AuditLog;
import com.ejobs.portal.model.Role;
import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.AuditLogRepository;
import com.ejobs.portal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first ADMIN account if the platform has none.
 *
 * <p>Runs in every profile by design. Registration refuses role=ADMIN and the admin
 * management endpoint requires an existing admin, so without this a fresh deployment
 * would have no way to ever get one.
 *
 * <p>Idempotent: if any administrator already exists it does nothing, so restarting the
 * application can never mint a second one.
 */
@Component
@Order(1) // Ahead of LocalDataSeeder, so the seeder sees a consistent picture.
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    static final String DEFAULT_EMAIL = "admin@ejobs.local";
    static final String DEFAULT_PASSWORD = "ChangeMe123!";

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(UserRepository userRepository,
                                AuditLogRepository auditLogRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${admin.bootstrap-email}") String bootstrapEmail,
                                @Value("${admin.bootstrap-password}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.info("Admin bootstrap skipped - an administrator account already exists");
            return;
        }

        String email = bootstrapEmail.trim().toLowerCase(Locale.ROOT);

        User admin = userRepository.save(User.builder()
                .fullName("Platform Administrator")
                .email(email)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ADMIN)
                .active(true)
                .build());

        auditLogRepository.save(AuditLog.builder()
                .eventType(AuditEventType.ADMIN_CREATED)
                .actorEmail("SYSTEM_BOOTSTRAP")
                .detail("initial admin bootstrap")
                .build());

        // Never log the password itself, only whether it is still the shipped default.
        log.info("Admin bootstrap created the initial administrator: {}", admin.getEmail());
        warnIfUsingDefaults();
    }

    private void warnIfUsingDefaults() {
        boolean defaultEmail = DEFAULT_EMAIL.equalsIgnoreCase(bootstrapEmail.trim());
        boolean defaultPassword = DEFAULT_PASSWORD.equals(bootstrapPassword);

        if (!defaultEmail && !defaultPassword) {
            return;
        }

        log.warn("""

                ***************************************************************
                 The bootstrap administrator is using shipped default {}.
                 These are published in the repository and are NOT a secret.
                 Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD, then
                 change this account's password before exposing the service.
                ***************************************************************""",
                credentialDescription(defaultEmail, defaultPassword));
    }

    private String credentialDescription(boolean defaultEmail, boolean defaultPassword) {
        if (defaultEmail && defaultPassword) {
            return "credentials";
        }
        return defaultEmail ? "email" : "password";
    }
}
