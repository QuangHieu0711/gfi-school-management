package com.gfi.backend.models.dtos.staff;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class StaffUpdateRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String aliasName;
    private String identityCode;
    private String gender;
    private LocalDate dateOfBirth;
    private Long ethnicityId;
    private Long religionId;
    private Long nationalityId;
    private String cccdNo;
    private LocalDate cccdIssueDate;
    private String cccdIssuePlace;
    private String phone;
    private String email;
    private String healthStatus;
    private String socialInsuranceNo;
    private String status;
    private String note;
}
