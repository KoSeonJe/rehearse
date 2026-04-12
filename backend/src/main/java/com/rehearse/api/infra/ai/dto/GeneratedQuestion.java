package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedQuestion {

    private String content;
    private String ttsContent;
    private String category;
    private int order;
    private String evaluationCriteria;
    private String questionCategory;
    private String modelAnswer;
    private String referenceType;
    private String followUpStrategy;
}
