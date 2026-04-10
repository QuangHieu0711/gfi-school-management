package com.gfi.backend.models.dtos.unit;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho API detail/create/update units.
 * Chứa đầy đủ thông tin unit.
 */
@Data
@Builder
public class UnitDetailDto {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String email;
    private Integer status;
}
