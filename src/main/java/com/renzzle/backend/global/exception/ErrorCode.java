package com.renzzle.backend.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // Global
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"G500","서버 내부에서 에러가 발생하였습니다"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "G400", "올바르지 않은 요청입니다"),
    GLOBAL_NOT_FOUND(HttpStatus.NOT_FOUND, "G404", "결과를 찾을 수 없습니다"),

    // SQL
    EMPTY_RESULT_ERROR(HttpStatus.NOT_FOUND, "S404", "요청의 결과가 존재하지 않습니다"),

    // Auth
    EXCEED_EMAIL_AUTH_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "A429", "이메일 인증 횟수를 초과했습니다"),
    NOT_VALID_CODE(HttpStatus.BAD_REQUEST, "A400", "올바르지 않은 인증코드입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
