package com.renzzle.backend.global.exception;

import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<ApiResponse<Object>> handleException(RuntimeException e) {
        return handleException(e, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    private ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, getErrorMessages(e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, "Invalid value for " + e.getName());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    private ResponseEntity<ApiResponse<Object>> handleEmptyResultDataAccessException(EmptyResultDataAccessException e) {
        return handleException(e, ErrorCode.EMPTY_RESULT_ERROR, ErrorCode.EMPTY_RESULT_ERROR.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    private ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return handleException(e, ErrorCode.CONSTRAINT_VIOLATION_ERROR, ErrorCode.CONSTRAINT_VIOLATION_ERROR.getMessage());
    }

    // IllegalArgumentException mostly comes from parsing request values
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    private ResponseEntity<ApiResponse<Object>> handleBadRequestException(Exception e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    private ResponseEntity<ApiResponse<Object>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return handleException(e, ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    private ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return handleException(e, ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }

    // Answered here, since a forward to /error loses the login and turns the 404 into a 401
    @ExceptionHandler(NoResourceFoundException.class)
    private ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        return handleException(e, ErrorCode.GLOBAL_NOT_FOUND, ErrorCode.GLOBAL_NOT_FOUND.getMessage());
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Object>> handleBusinessException(CustomException e) {
        return handleException(e, e.getErrorCode(), e.getMessage());
    }

    private ResponseEntity<ApiResponse<Object>> handleException(Exception e, ErrorCode errorCode, String message) {
        if (errorCode.getStatus().is5xxServerError() || errorCode == ErrorCode.CONSTRAINT_VIOLATION_ERROR) {
            log.error("[{}] {}", errorCode, e.getMessage(), e);
        } else {
            log.warn("[{}] {}: {}", errorCode, e.getClass().getSimpleName(), e.getMessage());
        }
        return ApiUtils.error(ErrorResponse.of(errorCode, message));
    }

}
