package com.renzzle.backend.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // Global
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"G500","Internal server error"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "G400", "Invalid request"),
    GLOBAL_NOT_FOUND(HttpStatus.NOT_FOUND, "G404", "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G405", "Unsupported HTTP method"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "G415", "Unsupported media type"),

    // SQL
    EMPTY_RESULT_ERROR(HttpStatus.NOT_FOUND, "S404", "No result found"),
    CONSTRAINT_VIOLATION_ERROR(HttpStatus.CONFLICT, "S409", "Database constraint violated"),

    // Auth
    EXCEED_EMAIL_AUTH_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "A429", "Too many email verification requests"),
    INVALID_EMAIL_AUTH_CODE(HttpStatus.UNAUTHORIZED, "A4010", "Invalid verification code"),
    INVALID_AUTH_VERITY_TOKEN(HttpStatus.UNAUTHORIZED, "A4011", "Invalid email verification token"),
    INVALID_EMAIL(HttpStatus.UNAUTHORIZED, "A4012", "Invalid email"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A4013", "Invalid password"),
    NOT_BEARER_GRANT_TYPE(HttpStatus.UNAUTHORIZED, "A4014", "Authorization type must be Bearer"),
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "A403", "Administrator privileges required"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A4090", "Email already registered"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "A4091", "Nickname already taken"),
    DUPLICATE_DEVICE(HttpStatus.CONFLICT, "A4092", "Device already registered"),

    // Jwt
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J4010", "Token expired"),
    MALFORMED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J4011", "Corrupted or malformed token"),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "J4012", "Unsupported token format"),
    ILLEGAL_TOKEN(HttpStatus.UNAUTHORIZED, "J4013", "Missing or malformed token"),
    CANNOT_PARSE_TOKEN(HttpStatus.UNAUTHORIZED, "J4014", "Failed to parse token"),

    // User
    CANNOT_LOAD_USER_INFO(HttpStatus.NOT_FOUND, "U4040", "Cannot load user information"),
    LEVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "U4041", "Level not found"),
    CANNOT_FIND_USER(HttpStatus.BAD_REQUEST, "U4000", "User not found"),
    INVALID_SUBSCRIPTION_REQUEST(HttpStatus.BAD_REQUEST, "U4001", "Invalid subscription information"),
    INSUFFICIENT_CURRENCY(HttpStatus.BAD_REQUEST, "U4002", "Not enough currency"),
    UNAUTHORIZED_ACTION(HttpStatus.UNAUTHORIZED, "U401", "Unauthorized user"),

    // Payment
    INVALID_PAYMENT_REQUEST(HttpStatus.BAD_REQUEST, "I4000", "Invalid payment request"),
    UNSUPPORTED_PAYMENT_PLATFORM(HttpStatus.BAD_REQUEST, "I4001", "Unsupported payment platform"),
    STORE_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "I4002", "Store payment verification failed"),
    RECEIPT_TRANSACTION_MISMATCH(HttpStatus.BAD_REQUEST, "I4003", "Receipt transaction does not match the request"),
    UNKNOWN_IAP_PRODUCT(HttpStatus.NOT_FOUND, "I4040", "Unregistered in-app purchase product"),
    ALREADY_PROCESSED_RECEIPT(HttpStatus.CONFLICT, "I4090", "Receipt already processed"),

    // Puzzle
    ALREADY_SOLVED_PUZZLE(HttpStatus.BAD_REQUEST, "P4000", "Puzzle already solved"),
    ALREADY_EXISTING_TRANSLATION(HttpStatus.BAD_REQUEST, "P4001", "Translation already exists for this language"),
    INVALID_SESSION_TTL(HttpStatus.BAD_REQUEST, "P4002", "Invalid session TTL"),
    IS_NOT_STARTED(HttpStatus.BAD_REQUEST, "P4003", "Ranked game not started"),
    NO_BOARD_STATUS(HttpStatus.BAD_REQUEST, "P4005", "Missing puzzle ID or board status"),
    INVALID_ANSWER_POSITION(HttpStatus.BAD_REQUEST, "P4006", "Answer coordinate out of range (a-o, 1-15)"),
    INVALID_RANK_PUZZLE_TYPE(HttpStatus.BAD_REQUEST, "R4004", "Invalid ranked puzzle type"),
    COMMUNITY_PUZZLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P4030", "No permission for this community puzzle"),
    CANNOT_FIND_COMMUNITY_PUZZLE(HttpStatus.NOT_FOUND, "P4040", "Community puzzle not found"),
    CANNOT_FIND_TRAINING_PUZZLE(HttpStatus.NOT_FOUND, "P4041", "Training puzzle not found"),
    NO_SUCH_TRAINING_PACK(HttpStatus.NOT_FOUND, "P4042", "Training pack not found"),
    NO_SUCH_TRAINING_PACKS(HttpStatus.NOT_FOUND, "P4043", "No training pack for that difficulty"),


    CANNOT_FIND_PUZZLE(HttpStatus.NOT_FOUND, "P4044", "Puzzle not found"),
    NO_SUCH_PACK_TRANSLATION(HttpStatus.NOT_FOUND, "P4044", "Pack translation not found"),
    NO_USER_PROGRESS_FOR_PACK(HttpStatus.NOT_FOUND, "P4045", "No progress for this pack"),
    CANNOT_FIND_RANK_PUZZLE(HttpStatus.NOT_FOUND, "P4046", "Ranked puzzle not found"),
    EMPTY_SESSION_DATA(HttpStatus.NOT_FOUND, "P4047", "Session data not found"),
    LATEST_PUZZLE_NOT_FOUND(HttpStatus.NOT_FOUND, "P4048", "No previous ranked puzzle"),
    TREND_PUZZLE_DUPLICATED(HttpStatus.CONFLICT, "P4090", "Duplicate trend puzzle"),
    EXCEED_DAILY_PUZZLE_UPLOAD(HttpStatus.TOO_MANY_REQUESTS, "P429", "Daily puzzle upload limit reached"),
    SESSION_ALREADY_ENDED(HttpStatus.GONE, "P4100", "Session already ended"),
    SESSION_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P5000", "Failed to generate session ID"),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
