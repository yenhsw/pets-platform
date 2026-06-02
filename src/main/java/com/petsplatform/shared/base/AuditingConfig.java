package com.petsplatform.shared.base;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Configuration để bật JPA Auditing.
 * Tự động điền createdAt, updatedAt, createdBy, updatedBy.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    /**
     * Cung cấp thông tin người dùng hiện tại cho auditing.
     * Trả về "system" nếu không có authentication context.
     * Khi tích hợp Spring Security, thay đổi implementation để lấy từ SecurityContext.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // TODO: Thay bằng SecurityContextHolder khi tích hợp Spring Security
            // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // return Optional.ofNullable(auth.getName());
            return Optional.of("system");
        };
    }
}
