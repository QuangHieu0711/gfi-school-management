package com.gfi.backend.models.dtos.staff;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffFamilyMemberDto {
    private Long id;
    private String relationType;
    private String fullName;
    private Integer birthYear;
    private String placeOfBirth;
    private String hometown;
    private String occupation;
    private String phone;
    private String workplace;
    private String address;
    private String note;
}
