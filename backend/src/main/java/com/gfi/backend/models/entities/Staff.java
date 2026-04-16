package com.gfi.backend.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "staffs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staffs_code", columnNames = "staff_code"),
        @UniqueConstraint(name = "uk_staffs_identity_code", columnNames = "identity_code")
})
@Getter
@Setter
public class Staff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User owns the relationship via staff_id FK
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "staff")
    private User user;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_staffs_unit"))
    private Unit unit;

    @Column(nullable = false, unique = true, length = 50)
    private String staffCode;

    @Column(unique = true, length = 50)
    private String identityCode;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(length = 255)
    private String aliasName;

    @Column(length = 20)
    private String gender;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private Long ethnicityId;

    @Column
    private Long religionId;

    @Column
    private Long nationalityId;

    @Column(length = 50)
    private String cccdNo;

    @Column
    private LocalDate cccdIssueDate;

    @Column(length = 255)
    private String cccdIssuePlace;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String healthStatus;

    @Column(length = 50)
    private String socialInsuranceNo;

    @Column
    private Long avatarFileId;

    @Column
    private Long signatureFileId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String note;
}
