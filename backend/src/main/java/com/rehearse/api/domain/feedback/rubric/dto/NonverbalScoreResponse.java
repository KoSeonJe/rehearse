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
    private final Integer d11Fluency;
    private final Integer d12Tone;
    private final Integer d13Posture;
    private final Integer d14Composure;
    private final String rawSignals;
    private final BigDecimal contextMultiplier;

    public static NonverbalScoreResponse from(NonverbalScore entity) {
        return NonverbalScoreResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterviewId())
                .turnId(entity.getTurnId())
                .d11Fluency(entity.getD11Fluency())
                .d12Tone(entity.getD12Tone())
                .d13Posture(entity.getD13Posture())
                .d14Composure(entity.getD14Composure())
                .rawSignals(entity.getRawSignals())
                .contextMultiplier(entity.getContextMultiplier())
                .build();
    }
}
