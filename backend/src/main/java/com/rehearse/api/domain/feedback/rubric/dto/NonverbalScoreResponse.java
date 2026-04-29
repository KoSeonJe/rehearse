package com.rehearse.api.domain.feedback.rubric.dto;

import com.rehearse.api.domain.feedback.rubric.entity.NonverbalScore;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class NonverbalScoreResponse {

    private final Long id;
    private final Long interviewId;
    private final Long turnId;
    private final Integer fluencyScore;
    private final Integer toneScore;
    private final Integer postureScore;
    private final Integer composureScore;
    private final String rawSignals;
    private final BigDecimal contextMultiplier;

    public static NonverbalScoreResponse from(NonverbalScore entity) {
        return NonverbalScoreResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterviewId())
                .turnId(entity.getTurnId())
                .fluencyScore(entity.getFluencyScore())
                .toneScore(entity.getToneScore())
                .postureScore(entity.getPostureScore())
                .composureScore(entity.getComposureScore())
                .rawSignals(entity.getRawSignals())
                .contextMultiplier(entity.getContextMultiplier())
                .build();
    }
}
