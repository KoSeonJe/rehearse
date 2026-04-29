package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NonverbalRubricScorer {

    private final NonverbalContextWeightsLoader weightsLoader;

    public NonverbalTurnScore score(SaveFeedbackRequest.NonverbalScore score,
                                    String questionCategory,
                                    String track,
                                    String mode,
                                    String difficulty) {
        if (score == null) {
            return NonverbalTurnScore.empty();
        }

        NonverbalContextWeight weight = weightsLoader.resolve(questionCategory, track, mode, difficulty);
        Integer d14 = weight.composureEnabled() ? score.getD14() : null;

        return new NonverbalTurnScore(
                score.getD11(),
                score.getD12(),
                score.getD13(),
                d14,
                rawSignals(score),
                weight.multiplier()
        );
    }

    private Map<String, Object> rawSignals(SaveFeedbackRequest.NonverbalScore score) {
        if (score.getRawSignals() != null && !score.getRawSignals().isEmpty()) {
            return Map.copyOf(score.getRawSignals());
        }
        Map<String, Object> raw = new HashMap<>();
        putIfPresent(raw, "d11", score.getD11());
        putIfPresent(raw, "d12", score.getD12());
        putIfPresent(raw, "d13", score.getD13());
        putIfPresent(raw, "d14", score.getD14());
        return Map.copyOf(raw);
    }

    private void putIfPresent(Map<String, Object> raw, String key, Integer value) {
        if (value != null) {
            raw.put(key, value);
        }
    }
}
