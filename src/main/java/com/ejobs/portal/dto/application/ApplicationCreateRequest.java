package com.ejobs.portal.dto.application;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

/**
 * {@code @URL} is a Hibernate Validator constraint, not a Jakarta one - it ships with
 * spring-boot-starter-validation.
 */
public record ApplicationCreateRequest(
        @NotBlank String coverNote,
        @NotBlank @URL String resumeUrl
) {
}
