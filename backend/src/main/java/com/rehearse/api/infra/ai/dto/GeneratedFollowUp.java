package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedFollowUp {

    private Boolean skip;
    private String skipReason;
    private String question;
    private String ttsQuestion;
    private String reason;
    private String type;
    private String modelAnswer;
    private String answerText;

    @JsonProperty("target_claim_idx")
    private Integer targetClaimIdx;

    @JsonProperty("selected_perspective")
    private String selectedPerspective;

    /** AI가 답변 불충분으로 후속질문 생성을 건너뛰도록 신호한 경우 true. */
    public boolean isSkipped() {
        return Boolean.TRUE.equals(skip);
    }

    /** Jackson 역직렬화 후 answerText를 추가한 새 인스턴스를 반환한다 (불변 복사). */
    public GeneratedFollowUp withAnswerText(String answerText) {
        GeneratedFollowUp copy = new GeneratedFollowUp();
        copy.skip = this.skip;
        copy.skipReason = this.skipReason;
        copy.question = this.question;
        copy.ttsQuestion = this.ttsQuestion;
        copy.reason = this.reason;
        copy.type = this.type;
        copy.modelAnswer = this.modelAnswer;
        copy.answerText = answerText;
        copy.targetClaimIdx = this.targetClaimIdx;
        copy.selectedPerspective = this.selectedPerspective;
        return copy;
    }
}
