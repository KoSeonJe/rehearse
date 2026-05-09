package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedRubricScoring - LLM 응답 매핑 시점 검증 + toDomain")
class GeneratedRubricScoringTest {

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("score 가 1~3 범위면 인스턴스를 생성한다")
        void should_construct_when_scores_in_range() {
            GeneratedRubricScoring g = new GeneratedRubricScoring(
                    "rubric-1",
                    List.of("clarity"),
                    Map.of("clarity", DimensionScore.of(2, "ok", "quote")),
                    null);

            assertThat(g.dimensionScores()).hasSize(1);
        }

        @Test
        @DisplayName("dimensionScores 가 비어있어도 통과한다 (adapter fallback 정책 보존)")
        void should_allow_empty_dimension_scores_for_adapter_fallback() {
            GeneratedRubricScoring g = new GeneratedRubricScoring(
                    "rubric-1", List.of(), Map.of(), null);
            assertThat(g.dimensionScores()).isEmpty();
        }

        @Test
        @DisplayName("score 가 null 이면 통과한다 (notApplicable 케이스)")
        void should_allow_null_score_for_not_applicable() {
            GeneratedRubricScoring g = new GeneratedRubricScoring(
                    "rubric-1",
                    List.of(),
                    Map.of("clarity", DimensionScore.notApplicable("LLM 응답에 차원 없음")),
                    null);
            assertThat(g.dimensionScores()).hasSize(1);
        }

        @Test
        @DisplayName("null 컬렉션은 빈 컬렉션으로 정규화된다")
        void should_normalize_null_collections_to_empty() {
            GeneratedRubricScoring g = new GeneratedRubricScoring("rubric-1", null, null, null);
            assertThat(g.scoredDimensions()).isEmpty();
            assertThat(g.dimensionScores()).isEmpty();
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("score 가 1 미만이면 거절한다")
        void should_reject_when_score_below_1() {
            assertThatThrownBy(() -> new GeneratedRubricScoring(
                    "rubric-1",
                    List.of("clarity"),
                    Map.of("clarity", DimensionScore.of(0, "ok", "quote")),
                    null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("score 가 3 초과면 거절한다")
        void should_reject_when_score_above_3() {
            assertThatThrownBy(() -> new GeneratedRubricScoring(
                    "rubric-1",
                    List.of("clarity"),
                    Map.of("clarity", DimensionScore.of(4, "ok", "quote")),
                    null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toDomain 변환")
    class ToDomain {

        @Test
        @DisplayName("도메인 객체로 등가 변환한다")
        void should_convert_to_domain() {
            GeneratedRubricScoring g = new GeneratedRubricScoring(
                    "rubric-1",
                    List.of("clarity"),
                    Map.of("clarity", DimensionScore.of(2, "ok", "quote")),
                    "L2");

            RubricScoringResult domain = g.toDomain();

            assertThat(domain.rubricId()).isEqualTo("rubric-1");
            assertThat(domain.scoredDimensions()).containsExactly("clarity");
            assertThat(domain.dimensionScores().get("clarity").score()).isEqualTo(2);
            assertThat(domain.levelFlag()).isEqualTo("L2");
        }
    }
}
