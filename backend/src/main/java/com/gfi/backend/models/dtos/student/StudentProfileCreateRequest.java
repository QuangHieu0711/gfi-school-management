package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentProfileCreateRequest {
    @Size(max = 255, message = "Doi tuong chinh sach toi da 255 ky tu")
    private String policyObject;
    @Size(max = 255, message = "Che do chinh sach toi da 255 ky tu")
    private String policyBenefit;
    @Size(max = 255, message = "Dien uu tien toi da 255 ky tu")
    private String priorityCategory;
    @Size(max = 255, message = "Dien hoc sinh toi da 255 ky tu")
    private String studentCategory;
    @Size(max = 255, message = "Khu vuc toi da 255 ky tu")
    private String regionCategory;
    @Size(max = 255, message = "Loai khuyet tat toi da 255 ky tu")
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
    @Size(max = 100, message = "He hoc ngoai ngu toi da 100 ky tu")
    private String foreignLanguageProgram;
    @Size(max = 255, message = "Chung chi ngoai ngu toi da 255 ky tu")
    private String foreignLanguageCertificate;
    @Size(max = 255, message = "Chung chi tin hoc toi da 255 ky tu")
    private String informaticsCertificate;
    @Size(max = 255, message = "Huong nghiep toi da 255 ky tu")
    private String careerOrientation;
    @Size(max = 255, message = "Dinh huong nghe toi da 255 ky tu")
    private String vocationalOrientation;
    private LocalDate joinedTeamDate;
    private LocalDate joinedUnionDate;
    private LocalDate joinedPartyDate;
    @Size(max = 100, message = "Ma he thong khac toi da 100 ky tu")
    private String otherSystemCode;
    @Size(max = 100, message = "Ma SSO toi da 100 ky tu")
    private String ssoCode;
}
