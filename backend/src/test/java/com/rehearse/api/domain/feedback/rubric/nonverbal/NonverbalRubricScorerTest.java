package com.rehearse.api.domain.feedback.rubric.nonverbal;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NonverbalRubricScorer")
class NonverbalRubricScorerTest {

    @Mock
    private NonverbalContextWeightsLoader weightsLoader;

    @Test
    @DisplayName("Lambda nonverbalScore를 D11~D14로 변환하고 context multiplier를 보존")
    void score_mapsLambdaScoreAndAppliesContextMultiplier() {
        NonverbalRubricScorer scorer = new NonverbalRubricScorer(weightsLoader);
        SaveFeedbackRequest.NonverbalScore score = createScore(3, 2, 1, 2);
        given(weightsLoader.resolve("BEHAVIORAL", null, null, "medium"))
                .willReturn(new NonverbalContextWeight(1.2, true));

        NonverbalTurnScore result = scorer.score(score, "BEHAVIORAL", null, null, "medium");

        assertThat(result.d11Fluency()).isEqualTo(3);
        assertThat(result.d12Tone()).isEqualTo(2);
        assertThat(result.d13Posture()).isEqualTo(1);
        assertThat(result.d14Composure()).isEqualTo(2);
        assertThat(result.contextMultiplier()).isEqualTo(1.2);
        assertThat(result.rawSignals()).containsEntry("d11", 3);
    }

    @Test
    @DisplayName("context가 D14 비활성화이면 Lambda d14 값도 저장하지 않음")
    void score_disabledComposure_omitsD14() {
        NonverbalRubricScorer scorer = new NonverbalRubricScorer(weightsLoader);
        SaveFeedbackRequest.NonverbalScore score = createScore(3, 3, 3, 3);
        given(weightsLoader.resolve("RESUME_BASED", "RESUME_BASED", "PLAYGROUND", "hard"))
                .willReturn(new NonverbalContextWeight(0.9, false));

        NonverbalTurnScore result = scorer.score(score, "RESUME_BASED", "RESUME_BASED", "PLAYGROUND", "hard");

        assertThat(result.d14Composure()).isNull();
        assertThat(result.contextMultiplier()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("nonverbalScore가 없으면 empty score를 반환")
    void score_nullScore_returnsEmpty() {
        NonverbalRubricScorer scorer = new NonverbalRubricScorer(weightsLoader);

        NonverbalTurnScore result = scorer.score(null, "CS_FUNDAMENTAL", null, null, "easy");

        assertThat(result.hasAnyScore()).isFalse();
    }

    private SaveFeedbackRequest.NonverbalScore createScore(Integer d11, Integer d12, Integer d13, Integer d14) {
        SaveFeedbackRequest.NonverbalScore score = new SaveFeedbackRequest.NonverbalScore();
        ReflectionTestUtils.setField(score, "d11", d11);
        ReflectionTestUtils.setField(score, "d12", d12);
        ReflectionTestUtils.setField(score, "d13", d13);
        ReflectionTestUtils.setField(score, "d14", d14);
        ReflectionTestUtils.setField(score, "rawSignals", Map.of("d11", d11, "d12", d12, "d13", d13));
        return score;
    }
}
