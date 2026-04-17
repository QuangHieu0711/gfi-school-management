package com.gfi.backend.models.dtos.staff;

import lombok.Data;

@Data
public class StaffAddressRequest {
    private Long provinceId;
    private Long districtId;
    private Long wardId;
    private String hamletName;
    private String detailAddress;
    private String fullAddress;
}
