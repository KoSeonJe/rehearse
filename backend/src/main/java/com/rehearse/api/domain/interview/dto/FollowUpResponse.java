package com.rehearse.api.domain.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class FollowUpResponse {

    private final Long questionId;
    private final String question;
    private final String ttsQuestion;
    private final String reason;
    private final String type;
    private final String answerText;
    private final String bestAnswer;
    private final boolean skip;
    private final String skipReason;
    private final boolean presentToUser;
    private final boolean followUpExhausted;

    public static FollowUpResponse aiSkip(String answerText, String skipReason) {
        return FollowUpResponse.builder()
                .answerText(answerText)
                .skip(true)
                .skipReason(skipReason)
                .presentToUser(false)
                .followUpExhausted(false)
                .build();
    }
}
