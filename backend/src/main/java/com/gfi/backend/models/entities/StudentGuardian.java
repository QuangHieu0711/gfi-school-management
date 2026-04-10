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
@Table(name = "student_guardians")
@Getter
@Setter
public class StudentGuardian extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_guardians_student"))
    private Student student;

    @Column(nullable = false, length = 20)
    private String guardianType;

    @Column(length = 255)
    private String fullName;

    @Column
    private Integer birthYear;

    @Column(length = 255)
    private String occupation;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String identityNumber;

    @Column
    private Boolean isEthnic;
}
