package com.gfi.backend.models.dtos.menu;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuListItemDto {
    private Long id;
    private String code;
    private String name;
    private String parentCode;
    private Integer ordinal;
    private String icon;
    private String url;
}
