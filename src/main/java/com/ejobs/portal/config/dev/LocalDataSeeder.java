package com.ejobs.portal.config.dev;

import com.ejobs.portal.model.EmploymentType;
import com.ejobs.portal.model.Job;
import com.ejobs.portal.model.JobStatus;
import com.ejobs.portal.model.Role;
import com.ejobs.portal.model.User;
import com.ejobs.portal.repository.JobRepository;
import com.ejobs.portal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Dev-only fixture data. Active only under the "local" profile, so it can never run in
 * a deployed environment even if the jar ships with it:
 *
 * <pre>SPRING_PROFILES_ACTIVE=local java -jar target/portal-0.0.1-SNAPSHOT.jar</pre>
 *
 * <p>Seeds nothing if any users already exist, so restarts are idempotent and hand-made
 * local data is never clobbered.
 */
@Component
@Profile("local")
@Order(2) // After AdminBootstrapRunner, which has already created the administrator.
public class LocalDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSeeder.class);

    private static final String EMPLOYER_EMAIL = "employer@test.com";
    private static final String SEEKER_EMAIL = "seeker@test.com";
    private static final String PASSWORD = "Test1234!";

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDataSeeder(UserRepository userRepository,
                           JobRepository jobRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Keyed on this fixture's own accounts rather than a bare count:
        // AdminBootstrapRunner has already inserted the administrator, so count() > 0 is
        // true on a brand new database and would skip seeding entirely.
        if (userRepository.existsByEmail(EMPLOYER_EMAIL)
                || userRepository.existsByEmail(SEEKER_EMAIL)
                || jobRepository.count() > 0) {
            log.info("Local seed skipped - fixture data already present");
            return;
        }

        User employer = userRepository.save(User.builder()
                .fullName("Acme Recruiting")
                .email(EMPLOYER_EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(Role.EMPLOYER)
                .active(true)
                .build());

        User seeker = userRepository.save(User.builder()
                .fullName("Sam Seeker")
                .email(SEEKER_EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(Role.JOB_SEEKER)
                .active(true)
                .build());

        // No admin is seeded here. AdminBootstrapRunner owns that, in every profile, so
        // that startup bootstrap and the admin-only API remain the only two ways an
        // ADMIN account can come into existence.

        Job active = Job.builder()
                .employer(employer)
                .title("Senior Backend Engineer")
                .department("Engineering")
                .location("Accra, Ghana")
                .employmentType(EmploymentType.REMOTE)
                .salaryRange("5000 - 8000 USD")
                .description("Build and operate the e-JOBS platform services: Spring Boot, "
                        + "PostgreSQL, and a good deal of API design.")
                .deadline(LocalDate.now().plusMonths(2))
                .status(JobStatus.ACTIVE)
                .build();

        Job draft = Job.builder()
                .employer(employer)
                .title("Product Designer")
                .department("Design")
                .location("Kumasi, Ghana")
                .employmentType(EmploymentType.FULL_TIME)
                .salaryRange("3000 - 4500 USD")
                .description("Own the end-to-end design of the applicant experience. "
                        + "Not yet announced.")
                .deadline(LocalDate.now().plusMonths(3))
                .status(JobStatus.DRAFT)
                .build();

        jobRepository.saveAll(List.of(active, draft));

        log.info("""
                Local seed complete:
                  employer : {} / {}  (owns 1 ACTIVE + 1 DRAFT job)
                  seeker   : {} / {}
                  admin    : see AdminBootstrapRunner (admin.bootstrap-email)""",
                employer.getEmail(), PASSWORD, seeker.getEmail(), PASSWORD);
    }
}
