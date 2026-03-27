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

    public GeneratedFollowUp withAnswerText(String answerText) {
        this.answerText = answerText;
        return this;
    }
}
