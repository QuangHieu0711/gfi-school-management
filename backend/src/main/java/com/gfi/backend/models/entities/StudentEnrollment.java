package com.gfi.backend.models.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "student_enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_enrollment", columnNames = { "student_id", "school_year_id" })
})
public class StudentEnrollment extends BaseEntity {

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

    @Column
    private Integer sessionsPerWeek;

    // 0 - Hoc ca ngay, 1 - Ban tru, 2 - Noi tru
    @Column
    private Integer studyMode;

    @Column
    private Boolean isBoarding;

    @Column
    private Boolean isTwoSessionsPerDay;

    @PrePersist
    public void prePersist() {
        super.prePersist();
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
}
