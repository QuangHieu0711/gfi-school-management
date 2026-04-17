package com.gfi.backend.models.dtos.staff;

import lombok.Data;

@Data
public class StaffFamilyMemberRequest {
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
