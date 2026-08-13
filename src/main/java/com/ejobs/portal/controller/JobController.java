package com.ejobs.portal.controller;

import com.ejobs.portal.dto.job.JobCreateRequest;
import com.ejobs.portal.dto.job.JobResponse;
import com.ejobs.portal.dto.job.JobStatusUpdateRequest;
import com.ejobs.portal.dto.job.JobSummaryResponse;
import com.ejobs.portal.dto.job.JobUpdateRequest;
import com.ejobs.portal.model.EmploymentType;
import com.ejobs.portal.config.security.AppUserPrincipal;
import com.ejobs.portal.service.JobService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Job posting and discovery (FR-JOB-01 to FR-JOB-05)")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a job posting",
            description = "FR-JOB-01. Employer only. The posting starts in DRAFT status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Caller is not an employer")
    })
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobCreateRequest request,
                                              Authentication authentication) {
        JobResponse created = jobService.createJob(currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a job posting",
            description = "FR-JOB-02. Owning employer only. Omitted fields are left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "403", description = "Not the owning employer"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobResponse> update(@PathVariable UUID id,
                                              @Valid @RequestBody JobUpdateRequest request,
                                              Authentication authentication) {
        return ResponseEntity.ok(jobService.updateJob(id, currentUserId(authentication), request));
    }

    @PatchMapping("/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change job status",
            description = "FR-JOB-05. Owning employer only. Allowed: DRAFT -> ACTIVE -> CLOSED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "403", description = "Not the owning employer"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "409", description = "Illegal status transition")
    })
    public ResponseEntity<JobResponse> updateStatus(@PathVariable UUID id,
                                                    @Valid @RequestBody JobStatusUpdateRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(
                jobService.updateStatus(id, currentUserId(authentication), request));
    }

    @GetMapping
    @Operation(summary = "Search active jobs",
            description = "FR-JOB-03 / FR-JOB-04. Public. Every filter is optional; "
                    + "keyword matches title or description, case-insensitively.")
    public Page<JobSummaryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType type,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return jobService.getActiveJobs(keyword, location, type, pageable);
    }

    /**
     * Declared before {@code /{id}} for readability. Spring already prefers the literal
     * path, but the security rule for it must also sit ahead of the public
     * GET /api/jobs/** matcher or this endpoint would be world-readable.
     */
    @GetMapping("/mine")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List my job postings",
            description = "Employer only. Returns the caller's own postings in every status, "
                    + "including DRAFT.")
    public List<JobResponse> mine(Authentication authentication) {
        return jobService.getJobsByEmployer(currentUserId(authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job by id",
            description = "FR-JOB-03. Public for ACTIVE and CLOSED postings. A DRAFT is "
                    + "visible only to the employer who owns it; everyone else gets 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Job not found, or a DRAFT "
                    + "belonging to another employer")
    })
    public JobResponse getById(@PathVariable UUID id, Authentication authentication) {
        // Anonymous is allowed here, so authentication may be null.
        return jobService.getJobById(id, AppUserPrincipal.idOrNull(authentication));
    }

    /** Read straight off the principal - no lookup, the JWT filter already loaded it. */
    private UUID currentUserId(Authentication authentication) {
        return AppUserPrincipal.requireId(authentication);
    }
}
