package com.rehearse.api.domain.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUpResponse {

    private final Long questionId;
    private final String question;
    private final String reason;
    private final String type;
    private final String answerText;
    private final String modelAnswer;
    /** AI가 답변 불충분으로 후속질문 생성을 건너뛴 경우 true. true일 때 questionId/question/reason/type/modelAnswer는 null. */
    private final boolean skip;
    private final String skipReason;
}
