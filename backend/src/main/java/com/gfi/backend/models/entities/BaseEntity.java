package com.gfi.backend.models.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.PrePersist;

/**
 * Base class cho tất cả entities.
 * Chứa các audit fields chung: createdAt, createdBy, updatedAt, updatedBy, deletedFlag, deletedAt, deletedBy
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @Column(length = 255, updatable = false)
    protected String createdBy;

    @UpdateTimestamp
    protected LocalDateTime updatedAt;

    @Column(length = 255)
    protected String updatedBy;

    // 0 - Hoạt động, 1 - Đã xóa
    @Column(nullable = false)
    protected Integer deletedFlag;

    @Column
    protected LocalDateTime deletedAt;

    @Column(length = 255)
    protected String deletedBy;

    @PrePersist
    protected void prePersist() {
        if (deletedFlag == null) {
            deletedFlag = 0;
        }
    }
}
