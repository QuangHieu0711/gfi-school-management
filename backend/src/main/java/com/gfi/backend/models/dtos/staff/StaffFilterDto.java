package com.gfi.backend.models.dtos.staff;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StaffFilterDto {
    private String staffCode;
    private String fullName;
    private Long unitId;
    private String status;
    private String gender;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
}
