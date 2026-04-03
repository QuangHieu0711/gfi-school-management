package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StudentFilterDto {
    private String fullName;
    private String firstName;
    private Integer studentStatus;
    private Long classId;
    private String moeCode;
    private Long gradeLevelId;
    private LocalDate dateOfBirth;
    private String gender;
    private String studentCode;
    private String otherSystemCode;
    private String fatherPhone;
    private String motherPhone;
    private String permanentProvinceName;
    private String permanentWardName;
}
