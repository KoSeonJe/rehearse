package com.rehearse.api.domain.interview.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "INTERVIEW_002", "잘못된 상태 전이입니다."),
    NOT_IN_PROGRESS(HttpStatus.CONFLICT, "INTERVIEW_003", "진행 중인 면접에서만 후속 질문을 생성할 수 있습니다."),
    QUESTION_GENERATION_NOT_COMPLETED(HttpStatus.CONFLICT, "INTERVIEW_004", "질문 생성이 완료되지 않아 면접을 시작할 수 없습니다."),
    QUESTION_GENERATION_NOT_FAILED(HttpStatus.CONFLICT, "INTERVIEW_005", "실패 상태의 질문 생성만 재시도할 수 있습니다."),
    ANSWER_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "INTERVIEW_006", "답변 텍스트 또는 오디오 파일이 필요합니다."),
    INVALID_TECH_STACK(HttpStatus.BAD_REQUEST, "INTERVIEW_007", "해당 직무에서 지원하지 않는 기술 스택입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "INTERVIEW_008", "해당 면접에 대한 접근 권한이 없습니다."),
    INVALID_INTERVIEW_TYPES(HttpStatus.BAD_REQUEST, "INTERVIEW_010", "면접 유형을 하나 이상 선택해야 합니다."),
    FOLLOWUP_DUPLICATE(HttpStatus.CONFLICT, "INTERVIEW_011", "이미 처리 중인 후속 질문이 있습니다. 잠시 후 다시 시도해주세요."),
    RETRY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "INTERVIEW_012", "재시도 한도를 초과했습니다."),
    RETRY_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "INTERVIEW_013", "재시도 대기 중입니다. 잠시 후 다시 시도해주세요."),
    RESUME_PLAN_RECOVERY_REQUIRED(HttpStatus.CONFLICT, "INTERVIEW_014", "이력서 분석 정보가 없어 처음부터 다시 시작해야 합니다."),
    CS_SUB_TOPIC_INVALID(HttpStatus.BAD_REQUEST, "INTERVIEW_015", "허용되지 않는 CS 주제입니다."),
    AUDIO_MIME_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "INTERVIEW_016", "허용되지 않는 오디오 형식입니다."),
    AUDIO_DURATION_EXCEEDED(HttpStatus.BAD_REQUEST, "INTERVIEW_017", "오디오 길이가 허용 범위를 초과했습니다."),
    AUDIO_MAGIC_BYTE_MISMATCH(HttpStatus.BAD_REQUEST, "INTERVIEW_018", "오디오 헤더가 일치하지 않습니다."),
    INTERVIEW_NOT_RESUME_BASED(HttpStatus.BAD_REQUEST, "INTERVIEW_019", "RESUME_BASED 인터뷰가 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
