package com.rehearse.api.domain.resume.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResumePlannerErrorCode implements ErrorCode {
    ORPHAN_CHAIN(HttpStatus.INTERNAL_SERVER_ERROR, "RESUME_PLANNER_001",
            "Plan의 chain_id가 Skeleton에 존재하지 않습니다."),
    INVALID_PLAN(HttpStatus.INTERNAL_SERVER_ERROR, "RESUME_PLANNER_003",
            "Planner 검증에 실패했습니다."),
    PLAN_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "RESUME_PLANNER_004",
            "면접 플랜 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
