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
        Integer composure = weight.composureEnabled() ? score.getComposure() : null;

        return new NonverbalTurnScore(
                score.getFluency(),
                score.getConfidenceTone(),
                score.getEyeContactPosture(),
                composure,
                rawSignals(score),
                weight.multiplier()
        );
    }

    private Map<String, Object> rawSignals(SaveFeedbackRequest.NonverbalScore score) {
        if (score.getRawSignals() != null && !score.getRawSignals().isEmpty()) {
            return Map.copyOf(score.getRawSignals());
        }
        Map<String, Object> raw = new HashMap<>();
        putIfPresent(raw, "fluency", score.getFluency());
        putIfPresent(raw, "confidence_tone", score.getConfidenceTone());
        putIfPresent(raw, "eye_contact_posture", score.getEyeContactPosture());
        putIfPresent(raw, "composure", score.getComposure());
        return Map.copyOf(raw);
    }

    private void putIfPresent(Map<String, Object> raw, String key, Integer value) {
        if (value != null) {
            raw.put(key, value);
        }
    }
}
