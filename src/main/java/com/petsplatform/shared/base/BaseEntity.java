package com.petsplatform.shared.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base entity cung cấp các trường chung cho tất cả entities.
 * Sử dụng @MappedSuperclass để các entity con kế thừa mapping.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Khóa chính - UUID để tránh ID collision khi scale.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Thời điểm tạo record.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm cập nhật cuối cùng.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Người tạo record.
     */
    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    /**
     * Người cập nhật cuối cùng.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Cờ xóa mềm - thay vì xóa record, đánh dấu là đã xóa.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
