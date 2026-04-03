package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentItemDto {
    private Long id;
    private String studentCode;
    private String fullName;
    private String firstName;
    private String moeCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String placeOfBirth;
    private String ethnicity;
    private String religion;
    private String nationality;
    private String mobilePhone;
    private String email;
    private String identityNumber;
    private LocalDate identityIssueDate;
    private String identityIssuePlace;
    private String healthInsuranceNumber;
    private String bloodGroup;
    private String boardingBook;
    private LocalDate admissionDate;
    private Integer studentStatus;
    private String admissionType;
    private Long unitId;
    private String unitName;
    private StudentEnrollmentItemDto enrollment;
    private List<StudentAddressItemDto> addresses;
    private List<StudentGuardianItemDto> guardians;
    private StudentProfileItemDto profile;
}
