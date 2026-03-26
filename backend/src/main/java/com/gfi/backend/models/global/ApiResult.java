package com.gfi.backend.models.global;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ApiResult<T> {

    // Trạng thái của kết quả, true nếu thành công, false nếu thất bại
    boolean status = false;

    // Thông điệp nội bộ
    String internalMessage;

    // Thông điệp hiển thị cho người dùng
    String userMessage;

    // Dữ liệu trả về
    T data;
    
    public static <T> ApiResult<T> success(T data, String userMessage) {
        ApiResult<T> result = new ApiResult<>();
        result.setStatus(true);
        result.setData(data);
        result.setUserMessage(userMessage);
        return result;
    }

    public static <T> ApiResult<T> fail(String userMessage) {
        ApiResult<T> result = new ApiResult<>();
        result.setStatus(false);
        result.setUserMessage(userMessage);
        return result;
    }
}
