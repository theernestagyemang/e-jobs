package com.ejobs.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled. Without this the audit retention sweep never runs - the annotation
 * is simply ignored, with no warning.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
