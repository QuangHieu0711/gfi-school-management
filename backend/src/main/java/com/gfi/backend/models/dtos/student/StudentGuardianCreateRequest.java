package com.gfi.backend.models.dtos.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentGuardianCreateRequest {

    @NotBlank(message = "Loai nguoi giam ho khong duoc de trong")
    @Size(max = 20, message = "Loai nguoi giam ho toi da 20 ky tu")
    private String guardianType;

    @Size(max = 255, message = "Ho ten toi da 255 ky tu")
    private String fullName;

    private Integer birthYear;

    @Size(max = 255, message = "Nghe nghiep toi da 255 ky tu")
    private String occupation;

    @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
    private String phone;

    @Email(message = "Email khong hop le")
    @Size(max = 255, message = "Email toi da 255 ky tu")
    private String email;

    @Size(max = 50, message = "So giay to toi da 50 ky tu")
    private String identityNumber;

    private Boolean isEthnic;
}
