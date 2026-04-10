package com.gfi.backend.models.entities;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "grade_level_subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_grade_level_subjects_grade_level_subject", columnNames = { "grade_level_id", "subject_id" })
})
@Getter
@Setter
public class GradeLevelSubject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grade_level_id", nullable = false, foreignKey = @ForeignKey(name = "fk_grade_level_subjects_grade_levels"))
    private GradeLevel gradeLevel;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false, foreignKey = @ForeignKey(name = "fk_grade_level_subjects_subjects"))
    private Subject subject;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 500)
    private String description;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) {
            status = 1;
        }
    }
}
