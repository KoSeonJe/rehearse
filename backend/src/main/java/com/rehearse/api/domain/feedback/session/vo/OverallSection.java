package com.rehearse.api.domain.feedback.session.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OverallSection(
        @JsonProperty("dimension_scores") Map<String, Double> dimensionScores,
        @JsonProperty("level_assessment") String levelAssessment,
        String narrative,
        String coverage
) {}
