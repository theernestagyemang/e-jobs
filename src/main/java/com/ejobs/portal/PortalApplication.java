package com.ejobs.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * VIA_DTO makes every {@code Page<T>} response serialize through Spring Data's
 * {@code PagedModel} wrapper rather than the raw {@code PageImpl}.
 *
 * <p>{@code PageImpl}'s JSON is an accident of its field layout - Spring logs a warning
 * about it and gives no compatibility guarantee across upgrades. PagedModel is a stable,
 * documented contract, at the cost of one breaking change now: the pagination counters
 * move from the root object into a nested "page" object.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class PortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortalApplication.class, args);
	}

}
