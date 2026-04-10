package com.gfi.backend.models.dtos.menu;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuDetailDto {
    private Long id;
    private Long parentId;
    private String parentCode;
    private String code;
    private String name;
    private String url;
    private String icon;
    private Integer ordinal;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
