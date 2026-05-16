package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedFollowUp(
        Boolean skip,
        @JsonProperty("skip_reason") String skipReason,
        String question,
        @JsonProperty("tts_question") String ttsQuestion,
        String reason,
        String type,
        @JsonProperty("best_answer") String bestAnswer,
        @JsonProperty("answer_text") String answerText,
        @JsonProperty("target_claim_idx") Integer targetClaimIdx
) {

    public GeneratedFollowUp {
        boolean skipped = Boolean.TRUE.equals(skip);
        if (!skipped && (question == null || question.isBlank())) {
            throw new IllegalArgumentException(
                    "GeneratedFollowUp.question 은 skip=false 일 때 비어있을 수 없습니다.");
        }
    }

    public boolean isSkipped() {
        return Boolean.TRUE.equals(skip);
    }

    public GeneratedFollowUp withAnswerText(String newAnswerText) {
        return new GeneratedFollowUp(skip, skipReason, question, ttsQuestion, reason, type,
                bestAnswer, newAnswerText, targetClaimIdx);
    }

    public static GeneratedFollowUp skipped(String reason) {
        return new GeneratedFollowUp(true, reason, null, null, null, null, null, null, null);
    }
}
