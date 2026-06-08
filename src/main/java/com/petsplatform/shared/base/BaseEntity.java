package com.petsplatform.shared.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Where;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity for all JPA entities in the platform.
 * Provides:
 * <ul>
 *   <li>UUID primary key — works across distributed systems without sequence conflicts</li>
 *   <li>Automatic timestamp auditing — createdAt / updatedAt via JPA @EntityListeners</li>
 *   <li>Auditor tracking — createdBy / updatedBy via AuditorContext ThreadLocal</li>
 *   <li>Soft delete — deleted flag with Hibernate @Where clause for transparent filtering</li>
 * </ul>
 *
 * <p>Usage: {@code public class User extends BaseEntity { ... }}
 *
 * <p>Requires:
 * <ul>
 *   <li>{@code @EnableJpaAuditing} on a configuration class</li>
 *   <li>{@code AuditorContext.setAuditor(username)} called in service/auth layer before save</li>
 * </ul>
 *
 * @see com.petsplatform.config.JpaAuditingConfig
 * @see com.petsplatform.shared.base.AuditorContext
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Where(clause = "deleted = false")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, nullable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Setter
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * Returns true if this entity has been soft-deleted.
     * Use this instead of checking {@code deleted == true} for null-safety.
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * Marks this entity as deleted (soft delete).
     * Does NOT call {@code em.remove()} — the entity remains in DB.
     */
    public void markDeleted() {
        this.deleted = true;
    }
}
