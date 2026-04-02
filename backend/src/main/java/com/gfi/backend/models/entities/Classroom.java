package com.gfi.backend.models.entities;

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
@Table(name = "classes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_classes_unit_grade_school_year_code", columnNames = { "unit_id", "grade_level_id", "school_year_id", "code" }),
        @UniqueConstraint(name = "uk_classes_unit_grade_school_year_name", columnNames = { "unit_id", "grade_level_id", "school_year_id", "name" })
})
@Data
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classes_units"))
    private Unit unit;

    @ManyToOne
    @JoinColumn(name = "grade_level_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classes_grade_levels"))
    private GradeLevel gradeLevel;

    @ManyToOne
    @JoinColumn(name = "school_year_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classes_school_years"))
    private SchoolYear schoolYear;

    @Column(nullable = false)
    private Integer status;

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
            status = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
