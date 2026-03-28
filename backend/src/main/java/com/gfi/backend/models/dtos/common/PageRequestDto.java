package com.gfi.backend.models.dtos.common;

import lombok.Data;

@Data
public class PageRequestDto<T> {
    private Integer pageSize = 10;
    private Integer pageNow = 1;
    private T filter;
}
