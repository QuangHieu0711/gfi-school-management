package com.gfi.backend.models.dtos.unit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnitItemDto {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String email;
    private Integer status;
}
