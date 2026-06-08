package com.petsplatform.config;

import com.petsplatform.shared.base.AuditorContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Supplies the current auditor to JPA's {@code @CreatedBy} / {@code @LastModifiedBy}.
 *
 * <p>Read from {@link AuditorContext} which is populated by:
 * <ul>
 *   <li>Security filter (for HTTP requests with authenticated user)</li>
 *   <li>Service layer (for batch/scheduled jobs)</li>
 *   <li>Any code that calls {@link AuditorContext#setAuditor(String)}</li>
 * </ul>
 *
 * @see com.petsplatform.shared.base.BaseEntity
 * @see com.petsplatform.shared.base.AuditorContext
 */
@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(AuditorContext.getAuditor());
    }
}
