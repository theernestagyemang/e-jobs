package com.ejobs.portal.service;

import com.ejobs.portal.dto.admin.AuditLogResponse;
import com.ejobs.portal.dto.admin.CreateAdminRequest;
import com.ejobs.portal.dto.admin.PlatformStatsResponse;
import com.ejobs.portal.dto.admin.UserStatusUpdateRequest;
import com.ejobs.portal.dto.admin.UserSummaryResponse;
import com.ejobs.portal.exception.ResourceNotFoundException;
import com.ejobs.portal.exception.UnauthorizedActionException;
import com.ejobs.portal.model.AuditEventType;
import com.ejobs.portal.model.AuditLog;
import com.ejobs.portal.model.JobStatus;
import com.ejobs.portal.model.Role;
import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.ApplicationRepository;
import com.ejobs.portal.repository.AuditLogRepository;
import com.ejobs.portal.repository.JobRepository;
import com.ejobs.portal.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/**
 * System Administrator capabilities: user management, the audit trail, and a platform
 * overview.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        JobRepository jobRepository,
                        ApplicationRepository applicationRepository,
                        AuditLogRepository auditLogRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Paginated user list, optionally narrowed to a single role. */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getAllUsers(Role role, Pageable pageable) {
        Page<User> users = (role == null)
                ? userRepository.findAll(pageable)
                : userRepository.findAllByRole(role, pageable);

        return users.map(this::toSummary);
    }

    /**
     * Activate or deactivate an account, recording the change in the audit trail.
     *
     * <p>Admins are off limits here: locking admins out of each other's accounts keeps
     * the endpoint simple and removes any chance of the last administrator being
     * disabled by mistake.
     */
    @Transactional
    public UserSummaryResponse setUserActiveStatus(UUID userId,
                                                   UserStatusUpdateRequest request,
                                                   String actingAdminEmail) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));

        if (target.getRole() == Role.ADMIN) {
            throw new UnauthorizedActionException(
                    "Administrator accounts cannot be activated or deactivated here");
        }

        boolean active = request.active();
        target.setActive(active);
        User saved = userRepository.save(target);

        auditLogRepository.save(AuditLog.builder()
                .eventType(active ? AuditEventType.USER_REACTIVATED
                        : AuditEventType.USER_DEACTIVATED)
                .actorEmail(actingAdminEmail)
                .detail(saved.getEmail())
                .build());

        return toSummary(saved);
    }

    /**
     * Creates another administrator. Together with AdminBootstrapRunner this is the only
     * way an ADMIN account can come into existence - registration refuses the role
     * outright, so privilege can only ever be granted by someone who already holds it.
     */
    @Transactional
    public UserSummaryResponse createAdmin(CreateAdminRequest request, String actingAdminEmail) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User admin = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .active(true)
                .build();

        User saved;
        try {
            // Same race as registration: only uk_users_email actually stops a concurrent
            // duplicate that slipped past the check above.
            saved = userRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        auditLogRepository.save(AuditLog.builder()
                .eventType(AuditEventType.ADMIN_CREATED)
                .actorEmail(actingAdminEmail)
                .detail(saved.getEmail())
                .build());

        return toSummary(saved);
    }

    /** Audit trail, newest first. */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAuditResponse);
    }

    /** Platform overview - plain COUNT queries, no aggregation table. */
    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        return new PlatformStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.EMPLOYER),
                userRepository.countByRole(Role.JOB_SEEKER),
                jobRepository.count(),
                jobRepository.countByStatus(JobStatus.ACTIVE),
                applicationRepository.count()
        );
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private AuditLogResponse toAuditResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEventType(),
                log.getActorEmail(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
