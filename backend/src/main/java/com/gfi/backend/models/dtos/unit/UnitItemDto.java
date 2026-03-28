package com.gfi.backend.models.dtos.unit;

import java.time.LocalDateTime;

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
    private Boolean status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
