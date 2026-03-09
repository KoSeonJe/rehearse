package com.devlens.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedFeedback {

    private double timestampSeconds;
    private String category;
    private String severity;
    private String content;
    private String suggestion;
}
