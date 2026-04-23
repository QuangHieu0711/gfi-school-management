package com.gfi.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_evaluations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_evaluation_class_subject_semester_student", columnNames = {
                "classroom_id", "subject_id", "semester_id", "student_id"
        })
})
@Getter
@Setter
public class StudentEvaluation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "classroom_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_evaluation_classroom"))
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_evaluation_subject"))
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_evaluation_semester"))
    private Semester semester;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_evaluation_student"))
    private Student student;

    @Column(name = "midterm_level", length = 10)
    private String midtermLevel;

    @Column(name = "midterm_remark", length = 2000)
    private String midtermRemark;

    @Column(name = "final_level", length = 10)
    private String finalLevel;

    @Column(name = "final_remark", length = 2000)
    private String finalRemark;
}
