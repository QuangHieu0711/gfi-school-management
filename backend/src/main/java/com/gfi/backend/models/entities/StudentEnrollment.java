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
@Table(name = "student_enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_enrollment", columnNames = { "student_id", "school_year_id" })
})
@Data
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_enrollments_student"))
    private Student student;

    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_enrollments_school_year"))
    private SchoolYear schoolYear;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_enrollments_class"))
    private Classroom classroom;

    @Column
    private LocalDate enrolledAt;

    @Column
    private Integer status;

    @Column
    private Boolean isRepeater;

    @Column(length = 50)
    private String sessionsPerWeek;

    @Column(length = 50)
    private String studyMode;

    @Column
    private Boolean isBoarding;

    @Column
    private Boolean isTwoSessionsPerDay;

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
        if (isRepeater == null) {
            isRepeater = Boolean.FALSE;
        }
        if (isBoarding == null) {
            isBoarding = Boolean.FALSE;
        }
        if (isTwoSessionsPerDay == null) {
            isTwoSessionsPerDay = Boolean.FALSE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
