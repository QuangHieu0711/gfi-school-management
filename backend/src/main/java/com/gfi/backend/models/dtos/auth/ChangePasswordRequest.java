package com.gfi.backend.models.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request đổi mật khẩu.
 * User đăng nhập bằng mật khẩu tạm rồi gọi API này để đổi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * Mật khẩu hiện tại (FE gửi dạng SHA256(plaintext))
     */
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    /**
     * Mật khẩu mới (FE gửi dạng SHA256(plaintext))
     */
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;
}
