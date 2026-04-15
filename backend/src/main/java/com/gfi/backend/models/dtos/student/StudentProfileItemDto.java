package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentProfileItemDto {
    private Long id;
    private String policyObject;
    private String policyBenefit;
    private String priorityCategory;
    private String studentCategory;
    private String regionCategory;
    private String disabilityType;
    private Boolean disabilityExemptEval;
    private Boolean supportTuitionCost;
    private Boolean resettlementArea;
    private Boolean housingSupport;
    private Boolean monthlyAllowance;
    private Boolean riceSupport;
    private Boolean followsMoeProgram;
    private Boolean canSwim;
    private Boolean learnsEthnicLanguage;
    private Boolean studiedKindergarten5yo;
    private Boolean needsVietnameseSupport;
    private Boolean hasVietnameseReinforcementMaterial;
    private Boolean hasEthnicTeachingAssistant;
    private Boolean hasParentInternet;
    private Boolean hasParentSmartphone;
    private String foreignLanguageProgram;
    private String foreignLanguageCertificate;
    private String informaticsCertificate;
    private String careerOrientation;
    private String vocationalOrientation;
    private LocalDate joinedTeamDate;
    private String otherSystemCode;
    private String ssoCode;
}
