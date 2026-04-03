package com.gfi.backend.models.dtos.student;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentCreateRequest {

    @NotBlank(message = "Ma hoc sinh khong duoc de trong")
    @Size(max = 50, message = "Ma hoc sinh toi da 50 ky tu")
    private String studentCode;

    @NotBlank(message = "Ho ten hoc sinh khong duoc de trong")
    @Size(max = 255, message = "Ho ten hoc sinh toi da 255 ky tu")
    private String fullName;

    @Size(max = 100, message = "Ten toi da 100 ky tu")
    private String firstName;

    @Size(max = 50, message = "Ma MOET toi da 50 ky tu")
    private String moeCode;

    @NotNull(message = "Ngay sinh khong duoc de trong")
    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Gioi tinh toi da 20 ky tu")
    private String gender;

    @Size(max = 255, message = "Noi sinh toi da 255 ky tu")
    private String placeOfBirth;

    @Size(max = 100, message = "Dan toc toi da 100 ky tu")
    private String ethnicity;

    @Size(max = 100, message = "Ton giao toi da 100 ky tu")
    private String religion;

    @Size(max = 100, message = "Quoc tich toi da 100 ky tu")
    private String nationality;

    @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
    private String mobilePhone;

    @Email(message = "Email khong hop le")
    @Size(max = 255, message = "Email toi da 255 ky tu")
    private String email;

    @Size(max = 50, message = "So CCCD/CMND toi da 50 ky tu")
    private String identityNumber;

    private LocalDate identityIssueDate;

    @Size(max = 255, message = "Noi cap toi da 255 ky tu")
    private String identityIssuePlace;

    @Size(max = 50, message = "So BHYT toi da 50 ky tu")
    private String healthInsuranceNumber;

    @Size(max = 50, message = "Nhom mau toi da 50 ky tu")
    private String bloodGroup;

    @Size(max = 100, message = "So dang bo toi da 100 ky tu")
    private String boardingBook;

    private LocalDate admissionDate;

    private Integer studentStatus;

    @Size(max = 100, message = "Hinh thuc trung tuyen toi da 100 ky tu")
    private String admissionType;

    @NotNull(message = "Don vi khong duoc de trong")
    private Long unitId;

    @NotNull(message = "Thong tin nhap hoc khong duoc de trong")
    @Valid
    private StudentEnrollmentCreateRequest enrollment;

    @Valid
    private List<StudentAddressCreateRequest> addresses;

    @Valid
    private List<StudentGuardianCreateRequest> guardians;

    @Valid
    private StudentProfileCreateRequest profile;
}
