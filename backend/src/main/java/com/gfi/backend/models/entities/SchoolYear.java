package com.gfi.backend.models.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "school_years", uniqueConstraints = {
        @UniqueConstraint(name = "uk_school_years_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_school_years_name", columnNames = "name")
})
@Data
public class SchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã năm học
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // Tên năm học
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    // Ngày bắt đầu năm học
    @Column(nullable = false)
    private LocalDate startDate;

    // Ngày kết thúc năm học
    @Column(nullable = false)
    private LocalDate endDate;

    // 0: Lập kế hoạch  1: Đang diễn ra, 2: Đã kết thúc
    @Column(nullable = false)
    private Integer status;

    // 0: Không phải năm học hiện tại, 1: Là năm học hiện tại
    @Column(nullable = false)
    private Boolean isCurrent;

    // Mô tả năm học
    @Column(length = 500)
    private String description;

    // Thông tin audit
    @Column
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String createdBy;

    @Column
    private LocalDateTime updatedAt;

    @Column(length = 255)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 0;
        }
        if (isCurrent == null) {
            isCurrent = Boolean.FALSE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
