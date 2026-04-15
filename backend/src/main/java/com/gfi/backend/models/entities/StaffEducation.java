package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_educations")
@Getter
@Setter
public class StaffEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staff_educations_staff"))
    private Staff staff;

    @Column(nullable = false, length = 30)
    private String educationType;

    @Column
    private Long levelId;

    @Column
    private Long majorId;

    @Column(length = 255)
    private String schoolName;

    @Column
    private Long trainingFormId;

    @Column
    private Integer graduationYear;

    @Column(length = 50)
    private String score;

    @Column(length = 50)
    private String frameworkLevel;

    @Column
    private Boolean isHighest;

    @Column(columnDefinition = "TEXT")
    private String note;
}
