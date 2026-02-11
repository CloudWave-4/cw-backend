package com.cjolivenetworks.CloudWave_Backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode {
    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 관리자에게 문의 바랍니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청 형식입니다."),

    //Redis queue 관련
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "REDIS_QUEUE_429", "요청이 많아 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;
}
