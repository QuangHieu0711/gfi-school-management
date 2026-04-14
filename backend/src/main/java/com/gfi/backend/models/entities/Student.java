package com.gfi.backend.models.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(name = "uk_students_student_code", columnNames = "student_code")
})
@Getter
@Setter
public class Student extends BaseEntity {

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

    //0 - Nam, 1 - Nữ
    @Column
    private Integer gender;

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

    @Column(length = 500)
    private String avatarUrl;

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

    @Column
    private Integer admissionType;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_students_unit"))
    private Unit unit;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (nationality == null || nationality.isBlank()) {
            nationality = "Việt Nam";
        }
    }
}
