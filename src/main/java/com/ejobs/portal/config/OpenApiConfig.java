package com.ejobs.portal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Must match the name used by {@code @SecurityRequirement} on the controllers -
     * Swagger resolves the padlock by this key, and a mismatch silently drops the
     * Authorize button.
     */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI eJobsOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, bearerJwtScheme()));
        // Deliberately no .addSecurityItem(...) here. A global requirement would apply
        // to every operation, padlocking /api/auth/login, /api/auth/register and the
        // public job search as though they needed a token. The protected operations
        // already declare @SecurityRequirement individually, which is enough for the
        // Authorize button to appear and keeps the documented contract honest.
    }

    private SecurityScheme bearerJwtScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("""
                        Paste the raw JWT returned by POST /api/auth/register or \
                        POST /api/auth/login. Swagger adds the "Bearer " prefix itself - \
                        do not type it.""");
    }

    private Info apiInfo() {
        return new Info()
                .title("e-JOBS API")
                .version("v1")
                .description("""
                        Backend for the e-JOBS portal: job seekers discover and apply to \
                        postings, employers publish them and run the hiring pipeline.

                        **Architecture** - a conventional layered Spring Boot stack:

                        - `controller` - HTTP endpoints, request validation, OpenAPI contract
                        - `service` - business rules: ownership checks and status-transition \
                        guards live here, not in the controllers
                        - `repository` - Spring Data JPA over PostgreSQL
                        - `model` - JPA entities (`User`, `Job`, `Application`)
                        - `dto` - request/response records, kept separate so entities are \
                        never serialised directly
                        - `config.security` - stateless JWT filter chain, BCrypt, CORS
                        - `exception` - `@RestControllerAdvice` mapping every failure to a \
                        consistent `ApiError` body

                        **Authentication** - register or log in, copy the `token` from the \
                        response, then click **Authorize** above. Endpoints without a \
                        padlock are public.

                        **Roles** - `EMPLOYER` publishes jobs and moves applications through \
                        the pipeline; `JOB_SEEKER` applies and tracks their own applications.""")
                .contact(new Contact().name("e-JOBS"))
                .license(new License().name("Proprietary"));
    }
}
