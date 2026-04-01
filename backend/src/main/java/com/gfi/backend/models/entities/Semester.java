package com.gfi.backend.models.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.gfi.backend.models.enums.AcademicPeriodStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_semesters_school_years"))
    private SchoolYear schoolYear;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer semesterOrder;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademicPeriodStatus status;

    @Column(nullable = false)
    private Boolean isCurrent;

    @Column(length = 500)
    private String description;

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
            status = AcademicPeriodStatus.PLANNING;
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
