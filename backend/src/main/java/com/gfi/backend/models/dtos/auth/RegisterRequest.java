package com.gfi.backend.models.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String password; // FE gửi: BCrypt hash từ client
    private Long roleId; // Role mà user mới append (optional, default: user role)
}
