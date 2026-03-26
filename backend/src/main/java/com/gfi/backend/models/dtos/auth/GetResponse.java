package com.gfi.backend.models.dtos.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetResponse {
    private Long userId;
    private String fullName;
    private String apartmentNumber;
    private String relationship;
}
