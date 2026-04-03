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
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(name = "uk_students_student_code", columnNames = "student_code")
})
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String studentCode;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(length = 100)
    private String firstName;

    @Column(length = 50)
    private String moeCode;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 255)
    private String placeOfBirth;

    @Column(length = 100)
    private String ethnicity;

    @Column(length = 100)
    private String religion;

    @Column(length = 100)
    private String nationality;

    @Column(length = 20)
    private String mobilePhone;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String identityNumber;

    @Column
    private LocalDate identityIssueDate;

    @Column(length = 255)
    private String identityIssuePlace;

    @Column(length = 50)
    private String healthInsuranceNumber;

    @Column(length = 50)
    private String bloodGroup;

    @Column(length = 100)
    private String boardingBook;

    @Column
    private LocalDate admissionDate;

    @Column
    private Integer studentStatus;

    @Column(length = 100)
    private String admissionType;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_students_unit"))
    private Unit unit;

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
        if (nationality == null || nationality.isBlank()) {
            nationality = "Việt Nam";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
