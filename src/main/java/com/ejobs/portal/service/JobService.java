package com.ejobs.portal.service;

import com.ejobs.portal.dto.job.JobCreateRequest;
import com.ejobs.portal.dto.job.JobResponse;
import com.ejobs.portal.dto.job.JobStatusUpdateRequest;
import com.ejobs.portal.dto.job.JobSummaryResponse;
import com.ejobs.portal.dto.job.JobUpdateRequest;
import com.ejobs.portal.exception.ResourceNotFoundException;
import com.ejobs.portal.exception.UnauthorizedActionException;
import com.ejobs.portal.model.EmploymentType;
import com.ejobs.portal.model.Job;
import com.ejobs.portal.model.JobStatus;
import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.JobRepository;
import com.ejobs.portal.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * FR-JOB-01 to FR-JOB-04.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public JobService(JobRepository jobRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    /** FR-JOB-01: new postings always start in DRAFT. */
    @Transactional
    public JobResponse createJob(UUID employerId, JobCreateRequest request) {
        User employer = userRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer " + employerId + " not found"));

        Job job = Job.builder()
                .employer(employer)
                .title(request.title().trim())
                .department(request.department())
                .location(request.location().trim())
                .employmentType(request.employmentType())
                .salaryRange(request.salaryRange())
                .description(request.description())
                .deadline(request.deadline())
                .status(JobStatus.DRAFT)
                .build();

        return toResponse(jobRepository.save(job));
    }

    /** FR-JOB-02: partial update, owner only. A null field means "leave unchanged". */
    @Transactional
    public JobResponse updateJob(UUID jobId, UUID employerId, JobUpdateRequest request) {
        Job job = loadOwnedJob(jobId, employerId);

        if (request.title() != null) {
            job.setTitle(request.title().trim());
        }
        if (request.department() != null) {
            job.setDepartment(request.department());
        }
        if (request.location() != null) {
            job.setLocation(request.location().trim());
        }
        if (request.employmentType() != null) {
            job.setEmploymentType(request.employmentType());
        }
        if (request.salaryRange() != null) {
            job.setSalaryRange(request.salaryRange());
        }
        if (request.description() != null) {
            job.setDescription(request.description());
        }
        if (request.deadline() != null) {
            job.setDeadline(request.deadline());
        }

        return toResponse(jobRepository.save(job));
    }

    /**
     * FR-JOB-05: the lifecycle is strictly DRAFT -> ACTIVE -> CLOSED. Anything else -
     * skipping ACTIVE, reopening a CLOSED job, or re-applying the current status -
     * is rejected.
     */
    @Transactional
    public JobResponse updateStatus(UUID jobId, UUID employerId, JobStatusUpdateRequest request) {
        Job job = loadOwnedJob(jobId, employerId);

        JobStatus current = job.getStatus();
        JobStatus target = request.status();

        if (!isTransitionAllowed(current, target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change status from " + current + " to " + target
                            + ". Allowed transitions are DRAFT -> ACTIVE -> CLOSED.");
        }

        job.setStatus(target);
        return toResponse(jobRepository.save(job));
    }

    /** FR-JOB-04: public search. Only ACTIVE postings are discoverable. */
    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> getActiveJobs(String keyword,
                                                  String location,
                                                  EmploymentType employmentType,
                                                  Pageable pageable) {
        return jobRepository
                .search(JobStatus.ACTIVE, blankToNull(keyword), blankToNull(location),
                        employmentType, pageable)
                .map(this::toSummary);
    }

    /**
     * FR-JOB-03: job detail, on a public endpoint.
     *
     * <p>A DRAFT posting has never been announced, so it is visible only to the employer
     * who owns it. Everyone else gets 404 rather than 403 - a 403 would confirm that a
     * hidden posting exists at that id. CLOSED stays readable: it was public once, and
     * applicants keep links to it.
     *
     * @param viewerId id of the caller, or null when the request is anonymous
     */
    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId, UUID viewerId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job " + jobId + " not found"));

        if (job.getStatus() == JobStatus.DRAFT
                && !job.getEmployer().getId().equals(viewerId)) {
            throw new ResourceNotFoundException("Job " + jobId + " not found");
        }

        return toResponse(job);
    }

    /** An employer's own board - every status, including DRAFT. */
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByEmployer(UUID employerId) {
        return jobRepository.findByEmployerId(employerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isTransitionAllowed(JobStatus current, JobStatus target) {
        return (current == JobStatus.DRAFT && target == JobStatus.ACTIVE)
                || (current == JobStatus.ACTIVE && target == JobStatus.CLOSED);
    }

    private Job loadOwnedJob(UUID jobId, UUID employerId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job " + jobId + " not found"));

        // Role alone is not enough - hasRole("EMPLOYER") passes for every employer.
        if (!job.getEmployer().getId().equals(employerId)) {
            throw new UnauthorizedActionException("You do not own job " + jobId);
        }
        return job;
    }

    /** Treat "?keyword=" as an absent filter rather than a search for the empty string. */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getEmployer().getId(),
                job.getEmployer().getFullName(),
                job.getTitle(),
                job.getDepartment(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getSalaryRange(),
                job.getDescription(),
                job.getDeadline(),
                job.getStatus(),
                job.getCreatedAt()
        );
    }

    private JobSummaryResponse toSummary(Job job) {
        return new JobSummaryResponse(
                job.getId(),
                job.getEmployer().getId(),
                job.getEmployer().getFullName(),
                job.getTitle(),
                job.getDepartment(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getSalaryRange(),
                job.getDeadline(),
                job.getStatus(),
                job.getCreatedAt()
        );
    }
}
