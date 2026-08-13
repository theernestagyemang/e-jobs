package com.ejobs.portal.controller;

import com.ejobs.portal.dto.admin.AuditLogResponse;
import com.ejobs.portal.dto.admin.CreateAdminRequest;
import com.ejobs.portal.dto.admin.PlatformStatsResponse;
import com.ejobs.portal.dto.admin.UserStatusUpdateRequest;
import com.ejobs.portal.dto.admin.UserSummaryResponse;
import com.ejobs.portal.model.Role;
import com.ejobs.portal.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "System Administrator: user management, audit trail, "
        + "platform oversight")
@SecurityRequirement(name = "bearerAuth")
// Defence in depth: SecurityConfig already restricts /api/admin/** to ROLE_ADMIN.
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(summary = "List users",
            description = "Paginated. Optional role filter: JOB_SEEKER, EMPLOYER or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
    })
    public Page<UserSummaryResponse> listUsers(
            @RequestParam(required = false) Role role,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.getAllUsers(role, pageable);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate a user",
            description = "A deactivated user cannot log in, and tokens already issued to "
                    + "them stop working. Administrator accounts cannot be changed here.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator, "
                    + "or the target is an administrator"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserSummaryResponse setUserStatus(@PathVariable UUID id,
                                             @Valid @RequestBody UserStatusUpdateRequest request,
                                             Authentication authentication) {
        return adminService.setUserActiveStatus(id, request, authentication.getName());
    }

    @PostMapping("/admins")
    @Operation(summary = "Create another administrator",
            description = "Together with the startup bootstrap this is the only way an "
                    + "ADMIN account can be created - self-registration with role=ADMIN "
                    + "is rejected outright.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrator created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<UserSummaryResponse> createAdmin(
            @Valid @RequestBody CreateAdminRequest request,
            Authentication authentication) {
        UserSummaryResponse created =
                adminService.createAdmin(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View the audit trail",
            description = "Newest first. Covers logins, registrations and account "
                    + "activation changes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit log page returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
    })
    public Page<AuditLogResponse> auditLogs(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminService.getAuditLogs(pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform overview",
            description = "User, job and application counts across the platform.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
    })
    public PlatformStatsResponse stats() {
        return adminService.getPlatformStats();
    }
}
