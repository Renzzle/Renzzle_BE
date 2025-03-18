package com.renzzle.backend.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // Global
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"G500","서버 내부에서 에러가 발생하였습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "G400", "올바르지 않은 요청입니다."),
    GLOBAL_NOT_FOUND(HttpStatus.NOT_FOUND, "G404", "결과를 찾을 수 없습니다."),

    // SQL
    EMPTY_RESULT_ERROR(HttpStatus.NOT_FOUND, "S404", "요청의 결과가 존재하지 않습니다."),
    CONSTRAINT_VIOLATION_ERROR(HttpStatus.CONFLICT, "S409", "데이터베이스 제약 조건을 위배하였습니다."),

    // Auth
    EXCEED_EMAIL_AUTH_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "A429", "이메일 인증 횟수를 초과했습니다."),
    INVALID_EMAIL_AUTH_CODE(HttpStatus.UNAUTHORIZED, "A4010", "유효하지 않은 인증코드입니다."),
    INVALID_AUTH_VERITY_TOKEN(HttpStatus.UNAUTHORIZED, "A4011", "유효하지 않은 회원가입 인증 토큰입니다."),
    INVALID_EMAIL(HttpStatus.UNAUTHORIZED, "A4012", "유효하지 않은 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A4013", "유효하지 않은 비밀번호입니다."),
    NOT_BEARER_GRANT_TYPE(HttpStatus.UNAUTHORIZED, "A4014", "인증 타입이 Bearer 타입이 아닙니다."),
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "A403", "관리자 권한이 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A4090", "이미 존재하는 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "A4091", "이미 존재하는 닉네임입니다."),

    // Jwt
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J4010", "만료된 토큰입니다."),
    MALFORMED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J4011", "손상되었거나 잘못된 형식의 토큰입니다."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J4012", "지원하지 않는 형식의 토큰입니다."),
    ILLEGAL_TOKEN(HttpStatus.UNAUTHORIZED, "J4013", "토큰이 없거나 잘못된 형식의 토큰입니다."),
    CANNOT_PARSE_TOKEN(HttpStatus.UNAUTHORIZED, "J4014", "토큰 파싱에 실패하였습니다."),

    //User
    CANNOT_LOAD_USER_INFO(HttpStatus.NOT_FOUND, "U4040", "사용자 정보를 불러올 수 없습니다."),
    LEVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "U4041", "해당 레벨을 찾을 수 없습니다."),
    CANNOT_FIND_PUZZLE(HttpStatus.NOT_FOUND, "U4042", "해당하는 사용자 퍼즐을 찾을 수 없습니다."),
    CANNOT_FIND_USER(HttpStatus.BAD_REQUEST, "U4000", "사용자를 찾을 수 없습니다."),
    INVALID_SUBSCRIPTION_REQUEST(HttpStatus.BAD_REQUEST, "U4001", "올바르지 않은 구독자 정보입니다."),
    INSUFFICIENT_CURRENCY(HttpStatus.BAD_REQUEST, "U4002", "재화가 부족합니다."),
    UNAUTHORIZED_ACTION(HttpStatus.UNAUTHORIZED, "U4010", "유효하지 않은 사용자입니다."),

    // Puzzle
    CANNOT_FIND_COMMUNITY_PUZZLE(HttpStatus.NOT_FOUND, "P4040", "해당하는 커뮤니티 퍼즐을 찾을 수 없습니다."),
    CANNOT_FIND_TRAINING_PUZZLE(HttpStatus.NOT_FOUND, "P4041", "해당하는 레슨 퍼즐을 찾을 수 없습니다."),
    NO_SUCH_TRAINING_PACK(HttpStatus.NOT_FOUND, "P4042", "해당하는 팩 정보를 찾을 수 없습니다."),

    //Rank
    SESSION_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "R5001", "세션 ID 생성에 실패하였습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
