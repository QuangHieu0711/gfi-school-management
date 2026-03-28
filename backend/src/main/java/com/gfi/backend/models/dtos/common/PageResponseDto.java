package com.gfi.backend.models.dtos.common;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResponseDto<T, F> {
    private Integer pageSize;
    private Integer pageNow;
    private F filter;
    private Integer pageTotal;
    private Long recordTotal;
    private List<T> items;
}
