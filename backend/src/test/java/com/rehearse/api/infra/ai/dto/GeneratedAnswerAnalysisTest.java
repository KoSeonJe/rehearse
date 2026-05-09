package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.AnswerFeedbackPerspective;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedAnswerAnalysis - LLM 응답 매핑 시점 검증 + toDomain")
class GeneratedAnswerAnalysisTest {

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("필수 필드와 범위 정상이면 인스턴스를 생성한다")
        void should_construct_when_fields_valid() {
            GeneratedAnswerAnalysis g = new GeneratedAnswerAnalysis(
                    1L, List.of(), List.of(AnswerFeedbackPerspective.TRADEOFF), List.of(),
                    3, RecommendedNextAction.DEEP_DIVE);

            assertThat(g.answerQuality()).isEqualTo(3);
            assertThat(g.recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
        }

        @Test
        @DisplayName("null 리스트는 빈 리스트로 정규화된다")
        void should_normalize_null_lists_to_empty() {
            GeneratedAnswerAnalysis g = new GeneratedAnswerAnalysis(
                    1L, null, null, null, 3, RecommendedNextAction.SKIP);

            assertThat(g.claims()).isEmpty();
            assertThat(g.missingPerspectives()).isEmpty();
            assertThat(g.unstatedAssumptions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("answerQuality 가 1 미만이면 거절한다")
        void should_reject_when_answer_quality_below_1() {
            assertThatThrownBy(() -> new GeneratedAnswerAnalysis(
                    1L, List.of(), List.of(), List.of(), 0, RecommendedNextAction.SKIP))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("answerQuality 가 5 초과면 거절한다")
        void should_reject_when_answer_quality_above_5() {
            assertThatThrownBy(() -> new GeneratedAnswerAnalysis(
                    1L, List.of(), List.of(), List.of(), 6, RecommendedNextAction.SKIP))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("recommendedNextAction 이 null 이면 거절한다")
        void should_reject_when_recommended_next_action_null() {
            assertThatThrownBy(() -> new GeneratedAnswerAnalysis(
                    1L, List.of(), List.of(), List.of(), 3, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toDomain 변환")
    class ToDomain {

        @Test
        @DisplayName("도메인 객체로 등가 변환한다")
        void should_convert_to_domain_with_same_values() {
            GeneratedAnswerAnalysis g = new GeneratedAnswerAnalysis(
                    42L, List.of(), List.of(AnswerFeedbackPerspective.TRADEOFF),
                    List.of("가정"), 4, RecommendedNextAction.DEEP_DIVE);

            AnswerAnalysis domain = g.toDomain();

            assertThat(domain.turnId()).isEqualTo(42L);
            assertThat(domain.answerQuality()).isEqualTo(4);
            assertThat(domain.recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
            assertThat(domain.missingPerspectives()).containsExactly(AnswerFeedbackPerspective.TRADEOFF);
            assertThat(domain.unstatedAssumptions()).containsExactly("가정");
        }
    }
}
