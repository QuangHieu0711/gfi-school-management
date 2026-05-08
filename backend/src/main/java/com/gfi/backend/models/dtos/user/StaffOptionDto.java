package com.gfi.backend.models.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOptionDto {
    private Long id;
    private String name;
    private String email;
    private String unitName;
    private String phone;
}