package com.ejobs.portal.service;

import com.ejobs.portal.dto.application.ApplicationCreateRequest;
import com.ejobs.portal.dto.application.ApplicationResponse;
import com.ejobs.portal.dto.application.ApplicationStatusUpdateRequest;
import com.ejobs.portal.exception.DuplicateApplicationException;
import com.ejobs.portal.exception.ResourceNotFoundException;
import com.ejobs.portal.exception.UnauthorizedActionException;
import com.ejobs.portal.model.Application;
import com.ejobs.portal.model.ApplicationStatus;
import com.ejobs.portal.model.Job;
import com.ejobs.portal.model.JobStatus;
import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.ApplicationRepository;
import com.ejobs.portal.repository.JobRepository;
import com.ejobs.portal.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * FR-APP-01 to FR-APP-05.
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    /** FR-APP-01 / FR-APP-02. */
    @Transactional
    public ApplicationResponse submitApplication(UUID applicantId,
                                                 UUID jobId,
                                                 ApplicationCreateRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job " + jobId + " not found"));

        // Only a published posting accepts applications - a DRAFT is unannounced and a
        // CLOSED one is finished.
        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Job " + jobId + " is not accepting applications (status " + job.getStatus() + ")");
        }

        // FR-APP-02: only an application that is still live blocks a new one. A previously
        // REJECTED candidate may apply again, matching the partial unique index.
        if (applicationRepository.existsByJobIdAndApplicantIdAndStatusNot(
                jobId, applicantId, ApplicationStatus.REJECTED)) {
            throw new DuplicateApplicationException(
                    "You already have an open application for this job ");
        }

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant " + applicantId + " not found"));

        Application application = Application.builder()
                .job(job)
                .applicant(applicant)
                .coverNote(request.coverNote())
                .resumeUrl(request.resumeUrl().trim())
                .status(ApplicationStatus.SUBMITTED)
                .build();

        try {
            // Two concurrent submissions can both clear the check above; only
            // uk_applications_job_applicant actually stops the second.
            return toResponse(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateApplicationException(
                    "You already have an open application for this job");
        }
    }

    /** FR-APP-03: every application across all of this employer's postings. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicantsForEmployer(UUID employerId) {
        return applicationRepository.findByJobEmployerId(employerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * FR-APP-04. Forward path is SUBMITTED -> UNDER_REVIEW -> SHORTLISTED, and REJECTED
     * is reachable from any state that is not already REJECTED.
     *
     * <p>REJECTED is treated as the only terminal state, so a shortlisted candidate can
     * still be turned down but a rejected one cannot be revived.
     */
    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID applicationId,
                                                       UUID employerId,
                                                       ApplicationStatusUpdateRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application " + applicationId + " not found"));

        // Role alone is not enough - this must be the employer who owns the posting.
        if (!application.getJob().getEmployer().getId().equals(employerId)) {
            throw new UnauthorizedActionException(
                    "Application " + applicationId + " does not belong to one of your jobs");
        }

        ApplicationStatus current = application.getStatus();
        ApplicationStatus target = request.status();

        if (!isTransitionAllowed(current, target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change application status from " + current + " to " + target
                            + ". Allowed: SUBMITTED -> UNDER_REVIEW -> SHORTLISTED, "
                            + "or REJECTED from any state except REJECTED.");
        }

        application.setStatus(target);
        return toResponse(applicationRepository.save(application));
    }

    /** FR-APP-05: the job seeker's own dashboard. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(UUID applicantId) {
        return applicationRepository.findByApplicantId(applicantId).stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isTransitionAllowed(ApplicationStatus current, ApplicationStatus target) {
        if (current == ApplicationStatus.REJECTED) {
            return false;
        }
        if (target == ApplicationStatus.REJECTED) {
            return true;
        }
        return (current == ApplicationStatus.SUBMITTED && target == ApplicationStatus.UNDER_REVIEW)
                || (current == ApplicationStatus.UNDER_REVIEW && target == ApplicationStatus.SHORTLISTED);
    }

    private ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getApplicant().getId(),
                application.getApplicant().getFullName(),
                application.getCoverNote(),
                application.getResumeUrl(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}
