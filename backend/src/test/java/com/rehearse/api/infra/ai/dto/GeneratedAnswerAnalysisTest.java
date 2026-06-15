package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedAnswerAnalysis — dimension_gaps null value 필터링")
class GeneratedAnswerAnalysisTest {

    @Test
    @DisplayName("dimension_gaps 의 null value 키는 도메인 Map 에서 제외된다 (strict schema 호환)")
    void dimensionGaps_filters_null_values() {
        Map<String, Integer> rawGaps = new HashMap<>();
        rawGaps.put("technical_depth", 2);
        rawGaps.put("problem_framing", null);
        rawGaps.put("chain_depth", null);
        rawGaps.put("experience_concreteness", 3);

        GeneratedAnswerAnalysis dto = new GeneratedAnswerAnalysis(
                "전사 텍스트",
                List.of(),
                rawGaps,
                "technical_depth",
                List.of(),
                RecommendedNextAction.DEEP_DIVE
        );

        assertThat(dto.dimensionGaps())
                .containsOnlyKeys("technical_depth", "experience_concreteness")
                .containsEntry("technical_depth", 2)
                .containsEntry("experience_concreteness", 3);
    }
}
