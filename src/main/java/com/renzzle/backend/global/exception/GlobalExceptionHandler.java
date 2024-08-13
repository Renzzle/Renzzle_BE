package com.renzzle.backend.global.exception;

import com.renzzle.backend.global.util.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<?> handleException(RuntimeException e) {
        return handleException(e, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<?> handleBusinessException(CustomException e) {
        return handleException(e, e.getErrorCode(), e.getMessage());
    }

    private ResponseEntity<?> handleException(Exception e, ErrorCode errorCode, String message) {
        log.error("error occurs! {}: {}", errorCode, e.getMessage());
        return ApiUtils.error(ErrorResponse.of(errorCode, message));
    }

}
