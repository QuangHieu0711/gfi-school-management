package com.gfi.backend.models.dtos.staff;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class StaffCreateRequest {
    @NotBlank(message = "Mã cán bộ không được để trống")
    private String staffCode;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotNull(message = "Đơn vị không được để trống")
    private Long unitId;

    private String aliasName;
    private String identityCode;
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
    private String status;
    private String note;
}
