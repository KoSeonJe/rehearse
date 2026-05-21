package com.rehearse.api.domain.feedback.rubric.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;

public record DimensionScore(
        Integer score,
        String observation,
        String evidenceQuote,
        DimensionStatus status
) {

    public DimensionScore {
        if (status == null) {
            status = DimensionStatus.OK;
        }
    }

    @JsonCreator
    public static DimensionScore of(
            @JsonProperty("score") Integer score,
            @JsonProperty("observation") String observation,
            @JsonProperty("evidence_quote") String evidenceQuote
    ) {
        return new DimensionScore(score, observation, evidenceQuote, DimensionStatus.OK);
    }

    public static DimensionScore notApplicable(String reason) {
        return new DimensionScore(null, reason, null, DimensionStatus.OK);
    }

    public static DimensionScore notEvaluable(String reason) {
        return new DimensionScore(null, reason, null, DimensionStatus.NOT_EVALUABLE);
    }
}
