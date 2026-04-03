package com.gfi.backend.models.dtos.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentAddressCreateRequest {

    @NotBlank(message = "Loai dia chi khong duoc de trong")
    @Size(max = 30, message = "Loai dia chi toi da 30 ky tu")
    private String addressType;

    @Size(max = 255, message = "Tinh/thanh toi da 255 ky tu")
    private String provinceName;

    @Size(max = 255, message = "Xa/phuong toi da 255 ky tu")
    private String wardName;

    @Size(max = 255, message = "Thon/xom toi da 255 ky tu")
    private String hamletName;

    @Size(max = 500, message = "Dia chi chi tiet toi da 500 ky tu")
    private String detailAddress;
}
