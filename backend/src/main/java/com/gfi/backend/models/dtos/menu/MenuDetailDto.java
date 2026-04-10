package com.gfi.backend.models.dtos.menu;


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
}
