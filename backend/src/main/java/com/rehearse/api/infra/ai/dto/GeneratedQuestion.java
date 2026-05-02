package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedQuestion {

    private String content;

    @JsonProperty("tts_content")
    private String ttsContent;

    private String category;
    private int order;

    @JsonProperty("evaluation_criteria")
    private String evaluationCriteria;

    @JsonProperty("question_category")
    private String questionCategory;

    @JsonProperty("model_answer")
    private String modelAnswer;

    @JsonProperty("reference_type")
    private String referenceType;

    private String followUpStrategy;
}
