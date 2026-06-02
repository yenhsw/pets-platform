package com.petsplatform.shared.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base class cho các Entity có soft delete.
 * Cung cấp timestamp và cờ isDeleted.
 */
@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    /**
     * Thời điểm xóa (nếu đã xóa mềm).
     */
    @JsonIgnore
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Người thực hiện xóa.
     */
    @JsonIgnore
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}
