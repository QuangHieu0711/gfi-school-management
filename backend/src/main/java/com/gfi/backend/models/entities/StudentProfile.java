package com.gfi.backend.models.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "student_profiles")
@Data
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_student_profiles_student"))
    private Student student;

    @Column(length = 255)
    private String policyObject;

    @Column(length = 255)
    private String policyBenefit;

    @Column(length = 255)
    private String priorityCategory;

    @Column(length = 255)
    private String studentCategory;

    @Column(length = 255)
    private String regionCategory;

    @Column(length = 255)
    private String disabilityType;

    @Column
    private Boolean disabilityExemptEval;

    @Column
    private Boolean supportTuitionCost;

    @Column
    private Boolean resettlementArea;

    @Column
    private Boolean housingSupport;

    @Column
    private Boolean monthlyAllowance;

    @Column
    private Boolean riceSupport;

    @Column
    private Boolean followsMoeProgram;

    @Column
    private Boolean canSwim;

    @Column
    private Boolean learnsEthnicLanguage;

    @Column
    private Boolean studiedKindergarten5yo;

    @Column
    private Boolean needsVietnameseSupport;

    @Column
    private Boolean hasVietnameseReinforcementMaterial;

    @Column
    private Boolean hasEthnicTeachingAssistant;

    @Column
    private Boolean hasParentInternet;

    @Column
    private Boolean hasParentSmartphone;

    @Column(length = 100)
    private String foreignLanguageProgram;

    @Column(length = 255)
    private String foreignLanguageCertificate;

    @Column(length = 255)
    private String informaticsCertificate;

    @Column(length = 255)
    private String careerOrientation;

    @Column(length = 255)
    private String vocationalOrientation;

    @Column
    private LocalDate joinedTeamDate;

    @Column
    private LocalDate joinedUnionDate;

    @Column
    private LocalDate joinedPartyDate;

    @Column(length = 100)
    private String otherSystemCode;

    @Column(length = 100)
    private String ssoCode;

    @PrePersist
    public void prePersist() {
        if (disabilityExemptEval == null) {
            disabilityExemptEval = Boolean.FALSE;
        }
        if (supportTuitionCost == null) {
            supportTuitionCost = Boolean.FALSE;
        }
        if (resettlementArea == null) {
            resettlementArea = Boolean.FALSE;
        }
        if (housingSupport == null) {
            housingSupport = Boolean.FALSE;
        }
        if (monthlyAllowance == null) {
            monthlyAllowance = Boolean.FALSE;
        }
        if (riceSupport == null) {
            riceSupport = Boolean.FALSE;
        }
        if (followsMoeProgram == null) {
            followsMoeProgram = Boolean.TRUE;
        }
        if (canSwim == null) {
            canSwim = Boolean.FALSE;
        }
        if (learnsEthnicLanguage == null) {
            learnsEthnicLanguage = Boolean.FALSE;
        }
        if (studiedKindergarten5yo == null) {
            studiedKindergarten5yo = Boolean.FALSE;
        }
        if (needsVietnameseSupport == null) {
            needsVietnameseSupport = Boolean.FALSE;
        }
        if (hasVietnameseReinforcementMaterial == null) {
            hasVietnameseReinforcementMaterial = Boolean.FALSE;
        }
        if (hasEthnicTeachingAssistant == null) {
            hasEthnicTeachingAssistant = Boolean.FALSE;
        }
        if (hasParentInternet == null) {
            hasParentInternet = Boolean.FALSE;
        }
        if (hasParentSmartphone == null) {
            hasParentSmartphone = Boolean.FALSE;
        }
    }
}
