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

    @NotBlank(message = "Mã học sinh không được để trống")
    @Size(max = 50, message = "Mã học sinh tối đa 50 ký tự")
    private String studentCode;

    @NotBlank(message = "Họ tên học sinh không được để trống")
    @Size(max = 255, message = "Họ tên học sinh tối đa 255 ký tự")
    private String fullName;

    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String firstName;

    @Size(max = 50, message = "Mã MOET tối đa 50 ký tự")
    private String moeCode;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;

    private Integer gender;

    @Size(max = 255, message = "Nơi sinh tối đa 255 ký tự")
    private String placeOfBirth;

    @Size(max = 100, message = "Dân tộc tối đa 100 ký tự")
    private String ethnicity;

    @Size(max = 100, message = "Tôn giáo tối đa 100 ký tự")
    private String religion;

    @Size(max = 100, message = "Quốc tịch tối đa 100 ký tự")
    private String nationality;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String mobilePhone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    private String avatarUrl;

    @Size(max = 50, message = "Số CCCD/CMND tối đa 50 ký tự")
    private String identityNumber;

    private LocalDate identityIssueDate;

    @Size(max = 255, message = "Nơi cấp tối đa 255 ký tự")
    private String identityIssuePlace;

    @Size(max = 50, message = "Số BHYT tối đa 50 ký tự")
    private String healthInsuranceNumber;

    @Size(max = 50, message = "Nhóm máu tối đa 50 ký tự")
    private String bloodGroup;

    @Size(max = 100, message = "Số đăng bộ tối đa 100 ký tự")
    private String boardingBook;

    private LocalDate admissionDate;

    private Integer studentStatus;

    private Integer admissionType;

    @NotNull(message = "Đơn vị không được để trống")
    private Long unitId;

    @NotNull(message = "Thông tin nhập học không được để trống")
    @Valid
    private StudentEnrollmentCreateRequest enrollment;

    @Valid
    private List<StudentAddressCreateRequest> addresses;

    @Valid
    private List<StudentGuardianCreateRequest> guardians;

    @Valid
    private StudentProfileCreateRequest profile;
}
