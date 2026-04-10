package com.gfi.backend.models.dtos.unit;

import lombok.Builder;
import lombok.Data;

/**
 * DTO cho API list/search units.
 * Chỉ chứa những thông tin tối thiểu để tránh over-fetching.
 */
@Data
@Builder
public class UnitListItemDto {
    private Long id;
    private String code;
    private String name;
    private String address;
    private Integer status;
}
