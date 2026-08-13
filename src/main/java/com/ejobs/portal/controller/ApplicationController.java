package com.ejobs.portal.controller;

import com.ejobs.portal.dto.application.ApplicationCreateRequest;
import com.ejobs.portal.dto.application.ApplicationResponse;
import com.ejobs.portal.dto.application.ApplicationStatusUpdateRequest;
import com.ejobs.portal.config.security.AppUserPrincipal;
import com.ejobs.portal.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Applications", description = "Applying and the hiring pipeline (FR-APP-01 to FR-APP-05)")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/api/jobs/{jobId}/applications")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Apply to a job",
            description = "FR-APP-01. Job seeker only. The job must be ACTIVE, and a seeker "
                    + "may apply to a given job only once (FR-APP-02).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Caller is not a job seeker"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "409", description = "Job not ACTIVE, or already applied")
    })
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable UUID jobId,
            @Valid @RequestBody ApplicationCreateRequest request,
            Authentication authentication) {

        ApplicationResponse created = applicationService.submitApplication(
                currentUserId(authentication), jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/employer/applications")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Applicant pipeline",
            description = "FR-APP-03. Employer only. Every application across the caller's postings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pipeline returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not an employer")
    })
    public List<ApplicationResponse> pipeline(Authentication authentication) {
        return applicationService.getApplicantsForEmployer(currentUserId(authentication));
    }

    @PatchMapping("/api/applications/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Move an application through the pipeline",
            description = "FR-APP-04. Owning employer only. Allowed: SUBMITTED -> UNDER_REVIEW "
                    + "-> SHORTLISTED, or REJECTED from any state except REJECTED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "403", description = "Application is not on one of your jobs"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Illegal status transition")
    })
    public ApplicationResponse updateStatus(@PathVariable UUID id,
                                            @Valid @RequestBody ApplicationStatusUpdateRequest request,
                                            Authentication authentication) {
        return applicationService.updateApplicationStatus(
                id, currentUserId(authentication), request);
    }

    @GetMapping("/api/applicants/me/applications")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "My applications",
            description = "FR-APP-05. Job seeker only. Everything the caller has applied to.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard returned"),
            @ApiResponse(responseCode = "403", description = "Caller is not a job seeker")
    })
    public List<ApplicationResponse> myApplications(Authentication authentication) {
        return applicationService.getMyApplications(currentUserId(authentication));
    }

    /** Read straight off the principal - no lookup, the JWT filter already loaded it. */
    private UUID currentUserId(Authentication authentication) {
        return AppUserPrincipal.requireId(authentication);
    }
}
