package com.renzzle.backend.global.exception;

import com.renzzle.backend.global.util.ApiUtils;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;
import static com.renzzle.backend.global.util.ErrorUtils.getStakeTrace;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<?> handleException(RuntimeException e) {
        return handleException(e, ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    private ResponseEntity<?> handleValidationException(ValidationException e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, getErrorMessages(e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return handleException(e, ErrorCode.VALIDATION_ERROR, e.getMessage());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    private ResponseEntity<?> handleEmptyResultDataAccessException(EmptyResultDataAccessException e) {
        return handleException(e, ErrorCode.EMPTY_RESULT_ERROR, ErrorCode.EMPTY_RESULT_ERROR.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    private ResponseEntity<?> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return handleException(e, ErrorCode.CONSTRAINT_VIOLATION_ERROR, ErrorCode.CONSTRAINT_VIOLATION_ERROR.getMessage());
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<?> handleBusinessException(CustomException e) {
        return handleException(e, e.getErrorCode(), e.getMessage());
    }

    private ResponseEntity<?> handleException(Exception e, ErrorCode errorCode, String message) {
        log.error(getStakeTrace(e));
        log.error("[{}] {}: {}", errorCode, e.getClass(), e.getMessage());
        return ApiUtils.error(ErrorResponse.of(errorCode, message));
    }

}
