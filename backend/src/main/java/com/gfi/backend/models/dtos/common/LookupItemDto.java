package com.gfi.backend.models.dtos.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LookupItemDto {
    private Long id;
    private String name;
}
