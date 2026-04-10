package com.gfi.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "grade_levels", uniqueConstraints = {
        @UniqueConstraint(name = "uk_grade_levels_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_grade_levels_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_grade_levels_grade_number", columnNames = "grade_number")
})
@Getter
@Setter
public class GradeLevel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, unique = true)
    private Integer gradeNumber;

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
