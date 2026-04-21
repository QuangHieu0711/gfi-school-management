package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_schoolyear_semester_class", columnNames = { "staff_id", "school_year_id", "semester_id", "class_id" })
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
    @JoinColumn(name = "semester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_teacher_assignments_semester"))
    private Semester semester;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = true, foreignKey = @ForeignKey(name = "fk_teacher_assignments_classroom"))
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = true, foreignKey = @ForeignKey(name = "fk_teacher_assignments_subject"))
    private Subject subject;
}
