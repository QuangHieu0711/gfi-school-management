package com.gfi.backend.models.dtos.student;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentAddressItemDto {
    private Long id;
    private String addressType;
    private String provinceName;
    private String wardName;
    private String hamletName;
    private String detailAddress;
}
