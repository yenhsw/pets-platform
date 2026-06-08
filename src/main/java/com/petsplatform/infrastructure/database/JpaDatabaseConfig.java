package com.petsplatform.infrastructure.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA / Database configuration.
 *
 * <p>Configures:
 * <ul>
 *   <li>Repository scanning — {@code @EnableJpaRepositories} for all modules</li>
 *   <li>Transaction management — {@code @EnableTransactionManagement}</li>
 *   <li>Custom repository base class — {@link AbstractBaseRepositoryImpl}</li>
 * </ul>
 *
 * <p>Naming strategy (in application.yml):
 * <ul>
 *   <li>Field: {@code firstName} → column: {@code first_name}</li>
 *   <li>Field: {@code userId} → column: {@code user_id}</li>
 * </ul>
 *
 * <p>Note: JPA Auditing ({@code @EnableJpaAuditing}) is configured separately
 * in {@code JpaAuditingConfig} to avoid duplicate bean conflicts.
 *
 * @see com.petsplatform.infrastructure.database.AbstractBaseRepositoryImpl
 * @see com.petsplatform.infrastructure.database.BaseRepository
 * @see com.petsplatform.config.JpaAuditingConfig
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.petsplatform",
    repositoryBaseClass = AbstractBaseRepositoryImpl.class
)
@EnableTransactionManagement
public class JpaDatabaseConfig {
    // All JPA/Hibernate settings are in application.yml.
}
