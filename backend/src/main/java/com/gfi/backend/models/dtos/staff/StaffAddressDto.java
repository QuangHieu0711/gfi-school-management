package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffAddressDto {
    private Long id;
    private String addressType;
    private Long provinceId;
    private Long districtId;
    private Long wardId;
    private String hamletName;
    private String detailAddress;
    private String fullAddress;
}
