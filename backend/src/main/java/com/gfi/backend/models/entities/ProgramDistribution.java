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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "program_distributions")
@Getter
@Setter
public class ProgramDistribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_program_distributions_school_years"))
    private SchoolYear schoolYear;

    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_program_distributions_semesters"))
    private Semester semester;

    @ManyToOne
    @JoinColumn(name = "classroom_id", nullable = false, foreignKey = @ForeignKey(name = "fk_program_distributions_classes"))
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false, foreignKey = @ForeignKey(name = "fk_program_distributions_subjects"))
    private Subject subject;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "period_ppct", length = 255)
    private String periodPpct;

    @Column(name = "lesson_name", nullable = false, length = 1000)
    private String lessonName;

    @Column(name = "note", length = 1000)
    private String note;
}
