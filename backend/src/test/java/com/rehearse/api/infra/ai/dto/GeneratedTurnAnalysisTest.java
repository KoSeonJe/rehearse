package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedTurnAnalysis - LLM 응답 매핑 시점 검증 + toDomain")
class GeneratedTurnAnalysisTest {

    private GeneratedAnswerAnalysis sampleAnalysis() {
        return new GeneratedAnswerAnalysis(
                1L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("answerAnalysis 가 있으면 인스턴스를 생성한다")
        void should_construct_when_answer_analysis_present() {
            GeneratedTurnAnalysis g = new GeneratedTurnAnalysis("hello", sampleAnalysis());
            assertThat(g.answerText()).isEqualTo("hello");
            assertThat(g.answerAnalysis()).isNotNull();
        }

        @Test
        @DisplayName("answerText null 은 빈 문자열로 정규화된다")
        void should_normalize_null_answer_text_to_empty_string() {
            GeneratedTurnAnalysis g = new GeneratedTurnAnalysis(null, sampleAnalysis());
            assertThat(g.answerText()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("answerAnalysis 가 null 이면 거절한다")
        void should_reject_when_answer_analysis_null() {
            assertThatThrownBy(() -> new GeneratedTurnAnalysis("text", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toDomain 변환")
    class ToDomain {

        @Test
        @DisplayName("도메인 객체로 등가 변환한다")
        void should_convert_to_domain() {
            GeneratedTurnAnalysis g = new GeneratedTurnAnalysis("answer", sampleAnalysis());
            TurnAnalysisResult domain = g.toDomain();
            assertThat(domain.answerText()).isEqualTo("answer");
            assertThat(domain.answerAnalysis().answerQuality()).isEqualTo(3);
        }
    }
}
