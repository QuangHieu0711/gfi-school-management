package com.gfi.backend.models.dtos.user;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class UserFilterDto {
    private String fullName;
    private Long roleId;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Long> unitId;
    private List<Long> userIds;
    private Integer status;
}
