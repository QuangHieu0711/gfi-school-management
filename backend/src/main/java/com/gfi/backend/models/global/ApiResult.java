package com.gfi.backend.models.global;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ApiResult<T> {

    boolean status = false;
    int code;
    String internalMessage;
    String userMessage;
    String traceID = UUID.randomUUID().toString();
    T data;

    public static <T> ApiResult<T> success(T data, String userMessage) {
        ApiResult<T> result = new ApiResult<>();
        result.setStatus(true);
        result.setCode(200);
        result.setData(data);
        result.setUserMessage(userMessage);
        return result;
    }

    public static <T> ApiResult<T> fail(String userMessage) {
        return fail(400, userMessage);
    }

    public static <T> ApiResult<T> fail(int code, String userMessage) {
        ApiResult<T> result = new ApiResult<>();
        result.setStatus(false);
        result.setCode(code);
        result.setUserMessage(userMessage);
        return result;
    }
}
