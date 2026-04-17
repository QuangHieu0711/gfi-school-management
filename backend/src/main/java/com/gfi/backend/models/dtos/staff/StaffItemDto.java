package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class StaffItemDto {
    private Long id;
    private String staffCode;
    private String fullName;
    private String aliasName;
    private Long unitId;
    private String gender;
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String status;
    private String cccdNo;
    private String avatarUrl;
}
