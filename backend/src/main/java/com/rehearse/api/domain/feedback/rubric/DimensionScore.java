package com.rehearse.api.domain.feedback.rubric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DimensionScore(
        Integer score,
        String observation,
        String evidenceQuote
) {

    @JsonCreator
    public static DimensionScore of(
            @JsonProperty("score") Integer score,
            @JsonProperty("observation") String observation,
            @JsonProperty("evidence_quote") String evidenceQuote
    ) {
        return new DimensionScore(score, observation, evidenceQuote);
    }

    public static DimensionScore notApplicable(String reason) {
        return new DimensionScore(null, reason, null);
    }
}
