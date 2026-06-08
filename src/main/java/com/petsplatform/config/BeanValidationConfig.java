package com.petsplatform.config;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.Set;

/**
 * Bean Validation configuration for the platform.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link LocalValidatorFactoryBean} — integrates with {@code ValidationMessages.properties}</li>
 *   <li>{@link MethodValidationPostProcessor} — enables {@code @Validated} on controller/service methods</li>
 * </ul>
 *
 * <p>With Hibernate Validator (the JSR-380 implementation included in spring-boot-starter-validation),
 * message interpolation uses {@code ${...}} for parameter substitution (e.g., {@code {min}}, {@code {value}}).
 *
 * <p>To enable method-level validation in controllers:
 * <pre>
 * {@code @Validated}
 * {@code @RestController}
 * public class UserController { ... }
 * </pre>
 *
 * @see com.petsplatform.shared.validation.ValidationHelper
 */
@Configuration
public class BeanValidationConfig {

    /**
     * Configures the JSR-380 validator factory.
     * Reads messages from {@code ValidationMessages.properties}.
     * Uses {@link ParameterMessageInterpolator} for ${} parameter substitution.
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setMessageInterpolator(new ParameterMessageInterpolator());
        return factory;
    }

    /**
     * Enables method-level validation for {@code @Validated} beans.
     * Required for {@code @NotNull} / {@code @Size} on {@code @RequestParam} and {@code @PathVariable}.
     *
     * <p>When using this, add {@code @Validated} on the controller class:
     * <pre>
     * {@code @Validated}
     * {@code @RestController}
     * public class UserController { ... }
     * </pre>
     *
     * <p>Also requires adding {@code spring-boot-starter-validation} dependency (already present).
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }
}
