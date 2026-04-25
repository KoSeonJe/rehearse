package com.rehearse.api.domain.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUpResponse {

    private final Long questionId;
    private final String question;
    private final String ttsQuestion;
    private final String reason;
    private final String type;
    private final String answerText;
    private final String modelAnswer;
    /**
     * DB 저장 생략 신호. true이면 이 턴은 Question 레코드 없이 처리된다.
     * UI 렌더 여부는 {@link #presentToUser}로 판단할 것.
     */
    private final boolean skip;
    private final String skipReason;
    /**
     * FE가 이 응답을 화면에 렌더링해야 하면 true.
     * AI 자체 skip(답변 불충분) 응답은 false, 사용자에게 돌려줄 안내 메시지(OFF_TOPIC/CLARIFY/GIVE_UP)는 true.
     */
    private final boolean presentToUser;
}
