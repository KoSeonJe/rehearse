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
    private final boolean skip;
    private final String skipReason;
    // FE 렌더링 신호. AI 자체 skip(답변 불충분)=false / 사용자 안내(OFF_TOPIC/CLARIFY/GIVE_UP)=true.
    private final boolean presentToUser;

    public static FollowUpResponse aiSkip(String answerText, String skipReason) {
        return FollowUpResponse.builder()
                .answerText(answerText)
                .skip(true)
                .skipReason(skipReason)
                .presentToUser(false)
                .build();
    }

    public static FollowUpResponse intentBranch(IntentBranchPayload payload) {
        return FollowUpResponse.builder()
                .question(payload.question())
                .ttsQuestion(payload.ttsQuestion())
                .reason(payload.reason())
                .type(payload.type())
                .answerText(payload.answerText())
                .skip(true)
                .skipReason(payload.skipReason())
                .presentToUser(true)
                .build();
    }

    public record IntentBranchPayload(
            String question,
            String ttsQuestion,
            String reason,
            String type,
            String skipReason,
            String answerText
    ) {}
}
