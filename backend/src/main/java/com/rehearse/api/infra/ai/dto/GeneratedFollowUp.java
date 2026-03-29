package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedFollowUp {

    private String question;
    private String reason;
    private String type;
    private String modelAnswer;
    private String answerText;

    /** Jackson 역직렬화 후 answerText를 추가한 새 인스턴스를 반환한다 (불변 복사). */
    public GeneratedFollowUp withAnswerText(String answerText) {
        GeneratedFollowUp copy = new GeneratedFollowUp();
        copy.question = this.question;
        copy.reason = this.reason;
        copy.type = this.type;
        copy.modelAnswer = this.modelAnswer;
        copy.answerText = answerText;
        return copy;
    }
}
