package com.rehearse.api.domain.feedback.rubric.service;

import java.util.Map;

public record NonverbalTurnScore(
        Integer d11Fluency,
        Integer d12Tone,
        Integer d13Posture,
        Integer d14Composure,
        Map<String, Object> rawSignals,
        double contextMultiplier
) {

    public static NonverbalTurnScore empty() {
        return new NonverbalTurnScore(null, null, null, null, Map.of(), 1.0);
    }

    public boolean hasAnyScore() {
        return d11Fluency != null || d12Tone != null || d13Posture != null || d14Composure != null;
    }
}
