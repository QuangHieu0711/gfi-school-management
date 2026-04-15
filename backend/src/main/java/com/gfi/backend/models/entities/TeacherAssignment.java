package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "teacher_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_schoolyear_class", columnNames = { "staff_id", "school_year_id", "class_id" })
})
@Getter
@Setter
public class TeacherAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_teacher_assignments_staff"))
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_teacher_assignments_schoolyear"))
    private SchoolYear schoolYear;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = true, foreignKey = @ForeignKey(name = "fk_teacher_assignments_classroom"))
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = true, foreignKey = @ForeignKey(name = "fk_teacher_assignments_subject"))
    private Subject subject;

    @Column
    private Boolean isHomeroom;

    @Column
    private Long departmentId;

    @Column(precision = 5, scale = 2)
    private BigDecimal teachingLoad;

    @Column(columnDefinition = "TEXT")
    private String note;
}
