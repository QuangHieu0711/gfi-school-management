package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class StaffDetailDto {
    private Long id;
    private Long userId;
    private Long unitId;
    private String staffCode;
    private String identityCode;
    private String fullName;
    private String aliasName;
    private String gender;
    private LocalDate dateOfBirth;
    private String ethnicityId;
    private String religionId;
    private String nationalityId;
    private String cccdNo;
    private LocalDate cccdIssueDate;
    private String cccdIssuePlace;
    private String phone;
    private String email;
    private String healthStatus;
    private String socialInsuranceNo;
    private Long avatarFileId;
    private Long signatureFileId;
    private String status;
    private String note;
}
