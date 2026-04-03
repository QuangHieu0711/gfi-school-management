package com.gfi.backend.models.dtos.student;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentGuardianItemDto {
    private Long id;
    private String guardianType;
    private String fullName;
    private Integer birthYear;
    private String occupation;
    private String phone;
    private String email;
    private String identityNumber;
    private Boolean isEthnic;
}
