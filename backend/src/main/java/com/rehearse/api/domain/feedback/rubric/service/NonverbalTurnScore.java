package com.rehearse.api.domain.feedback.rubric.service;

import java.util.Map;

public record NonverbalTurnScore(
        Integer fluency,
        Integer confidenceTone,
        Integer eyeContactPosture,
        Integer composure,
        Map<String, Object> rawSignals,
        double contextMultiplier
) {

    public static NonverbalTurnScore empty() {
        return new NonverbalTurnScore(null, null, null, null, Map.of(), 1.0);
    }

    public boolean hasAnyScore() {
        return fluency != null || confidenceTone != null || eyeContactPosture != null || composure != null;
    }
}
