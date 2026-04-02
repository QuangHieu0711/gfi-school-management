package com.gfi.backend.models.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "semesters", uniqueConstraints = {
        @UniqueConstraint(name = "uk_semesters_school_year_code", columnNames = { "school_year_id", "code" }),
        @UniqueConstraint(name = "uk_semesters_school_year_name", columnNames = { "school_year_id", "name" }),
        @UniqueConstraint(name = "uk_semesters_school_year_order", columnNames = { "school_year_id", "semester_order" })
})
@Data
public class Semester {

    // Khóa chính
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Năm học
    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_semesters_school_years"))
    private SchoolYear schoolYear;

    // Mã học kỳ
    @Column(nullable = false, length = 50)
    private String code;

    // Tên học kỳ
    @Column(nullable = false, length = 255)
    private String name;

    // Thứ tự học kỳ 
    @Column(nullable = false)
    private Integer semesterOrder;

    // Ngày bắt đầu học kỳ
    @Column(nullable = false)
    private LocalDate startDate;

    // Ngày kết thúc học kỳ
    @Column(nullable = false)
    private LocalDate endDate;

    // 0: Lap ke hoach
    @Column(nullable = false)
    private Integer status;

    // 0: Không phải học kỳ hiện tại, 1: Là học kỳ hiện tại
    @Column(nullable = false)
    private Boolean isCurrent;

    // Mô tả học kỳ
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
