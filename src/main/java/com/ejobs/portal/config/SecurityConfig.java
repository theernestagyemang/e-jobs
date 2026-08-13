package com.ejobs.portal.config;

import com.ejobs.portal.config.security.JwtAuthFilter;
import com.ejobs.portal.config.security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
// Required for the @PreAuthorize on AdminController. Without it those annotations are
// silently ignored and the class-level guard does nothing.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /** NFR-SEC-01: BCrypt with an explicit strength of 10. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /** FR-AUTH-03: needed by the login endpoint to authenticate email + password. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Local frontend dev servers.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // NFR-SEC-01: CSRF tokens are meaningless for a stateless bearer-token API.
                .csrf(csrf -> csrf.disable())
                // NFR-SEC-01: no HTTP session is ever created.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth

                        // ---- Public ----------------------------------------------------
                        // Spring Security also filters the ERROR dispatch. Without this,
                        // a 404/500 raised by a public endpoint is itself blocked and the
                        // caller sees a misleading 401 instead of the real error.
                        .requestMatchers("/error").permitAll()

                        // Render polls this unauthenticated to decide whether the
                        // container is live. Without this rule it falls through to
                        // anyRequest().authenticated(), Render sees 401 and never marks
                        // the deploy healthy. Safe to expose: management.endpoint.health
                        // .show-details=never keeps the body at {"status":"UP"}
                        // (NFR-SEC-02), and only the health endpoint is exposed at all.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // FR-AUTH-01 register, FR-AUTH-03 login, plus the password reset
                        // flow - a user who cannot sign in must be able to reach both.
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password")
                        .permitAll()

                        // OpenAPI docs - drop these two lines to lock docs down in prod.
                        .requestMatchers(HttpMethod.GET,
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()

                        // ---- Administrator-only --------------------------------------
                        // System Administrator: user management, audit trail, oversight.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ---- Employer-only -------------------------------------------
                        // FR-APP-03: applicant pipeline. MUST precede the public
                        // GET /api/jobs/** rule below, otherwise this leaks publicly.
                        .requestMatchers(HttpMethod.GET, "/api/jobs/*/applications")
                        .hasRole("EMPLOYER")

                        // An employer's own board, including DRAFT postings. Like the rule
                        // above, this MUST precede GET /api/jobs/** or it is world-readable.
                        .requestMatchers(HttpMethod.GET, "/api/jobs/mine").hasRole("EMPLOYER")

                        // FR-JOB-01 create, FR-JOB-02 update, FR-JOB-05 status transitions.
                        .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PUT, "/api/jobs/*").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PATCH, "/api/jobs/*/status").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.DELETE, "/api/jobs/*").hasRole("EMPLOYER")

                        // FR-APP-04: employer moves an application through the pipeline.
                        .requestMatchers(HttpMethod.PATCH, "/api/applications/*/status")
                        .hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/api/employer/**").hasRole("EMPLOYER")

                        // ---- Job seeker-only -----------------------------------------
                        // FR-APP-01 / FR-APP-02: submit an application.
                        .requestMatchers(HttpMethod.POST, "/api/jobs/*/applications")
                        .hasRole("JOB_SEEKER")
                        // FR-APP-05: "my applications" dashboard. This path must track
                        // ApplicationController exactly - a stale matcher does not deny,
                        // it falls through to anyRequest().authenticated(), which lets any
                        // logged-in employer read a job seeker's dashboard.
                        .requestMatchers(HttpMethod.GET, "/api/applicants/me/applications")
                        .hasRole("JOB_SEEKER")

                        // ---- Public discovery ----------------------------------------
                        // FR-JOB-03 / FR-JOB-04: anyone may browse and search jobs.
                        // Deliberately last of the /api/jobs rules - it is a broad
                        // wildcard and would otherwise shadow the employer rules above.
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/**").permitAll()

                        // ---- Everything else -----------------------------------------
                        .anyRequest().authenticated()
                )
                // FR-AUTH-04: populate the SecurityContext before the username/password filter.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // CRA/Next on 3000, Vite on 5173.
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
