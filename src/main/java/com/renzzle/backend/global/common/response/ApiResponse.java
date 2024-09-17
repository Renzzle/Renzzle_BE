package com.renzzle.backend.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.renzzle.backend.global.exception.ErrorResponse;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(@JsonProperty("isSuccess") Boolean isSuccess,
                             @JsonProperty(value = "response") T response,
                             @JsonProperty(value = "errorResponse") ErrorResponse errorResponse) {

    public static <T> ApiResponse<T> create(boolean isSuccess, T response, ErrorResponse errorResponse) {
        return new ApiResponse<>(isSuccess, response, errorResponse);
    }

}