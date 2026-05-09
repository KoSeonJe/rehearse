package com.rehearse.api.domain.resume.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeFallbackBestAnswers - 분량 / 가이드 톤 / 1인칭 placeholder 부재 / 구조 단서 가드")
class ResumeFallbackBestAnswersTest {

    @Nested
    @DisplayName("분량 검증 - 한국어 200~300자 범위")
    class LengthRange {

        @Test
        @DisplayName("opener_fallback_length_within_200_to_300_chars — OPENER 상수 길이 200~300자 범위")
        void opener_fallback_length_within_200_to_300_chars() {
            assertThat(ResumeFallbackBestAnswers.OPENER.length()).isBetween(200, 300);
        }

        @Test
        @DisplayName("playground_fallback_length_within_200_to_300_chars — PLAYGROUND 상수 길이 200~300자 범위")
        void playground_fallback_length_within_200_to_300_chars() {
            assertThat(ResumeFallbackBestAnswers.PLAYGROUND.length()).isBetween(200, 300);
        }

        @Test
        @DisplayName("interrogation_fallback_length_within_200_to_300_chars — INTERROGATION 상수 길이 200~300자 범위")
        void interrogation_fallback_length_within_200_to_300_chars() {
            assertThat(ResumeFallbackBestAnswers.INTERROGATION.length()).isBetween(200, 300);
        }
    }

    @Nested
    @DisplayName("가이드 톤 - LLM 실패 시 사용자 컨텍스트 모름. 답변 짤 단서 권유 톤만")
    class GuideTone {

        @Test
        @DisplayName("all_fallbacks_use_guide_tone — 3 상수 모두 권유 / 안내 어미 1+ 매치")
        void all_fallbacks_use_guide_tone() {
            assertThat(ResumeFallbackBestAnswers.OPENER)
                    .containsAnyOf("효과적입니다", "구조가", "마무리하면", "서술하세요", "정리하세요", "마무리하세요");
            assertThat(ResumeFallbackBestAnswers.PLAYGROUND)
                    .containsAnyOf("효과적입니다", "구조가", "마무리하면", "서술하세요", "정리하세요", "마무리하세요");
            assertThat(ResumeFallbackBestAnswers.INTERROGATION)
                    .containsAnyOf("효과적입니다", "구조가", "마무리하면", "서술하세요", "정리하세요", "마무리하세요");
        }
    }

    @Nested
    @DisplayName("1인칭 placeholder 부재 - 거짓 답변 예시 금지 (사용자 컨텍스트 무관 placeholder 출력 금지)")
    class NoFirstPersonPlaceholder {

        @Test
        @DisplayName("all_fallbacks_forbid_first_person_placeholder — 3 상수 모두 1인칭 답변 어미 0건")
        void all_fallbacks_forbid_first_person_placeholder() {
            assertThat(ResumeFallbackBestAnswers.OPENER)
                    .doesNotContain("저는 ", "본인이 직접 수행했습니다", "경험이 있습니다");
            assertThat(ResumeFallbackBestAnswers.PLAYGROUND)
                    .doesNotContain("저는 ", "본인이 직접 수행했습니다", "경험이 있습니다");
            assertThat(ResumeFallbackBestAnswers.INTERROGATION)
                    .doesNotContain("저는 ", "본인이 직접 수행했습니다", "경험이 있습니다");
        }
    }

    @Nested
    @DisplayName("구조 단서 - STAR / 결정 근거 / 트레이드오프 / 학습 1+ 매치")
    class StructureSignal {

        @Test
        @DisplayName("all_fallbacks_include_structure_signal — 3 상수 모두 구조 단서 어휘 1+ 매치")
        void all_fallbacks_include_structure_signal() {
            assertThat(ResumeFallbackBestAnswers.OPENER)
                    .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
            assertThat(ResumeFallbackBestAnswers.PLAYGROUND)
                    .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
            assertThat(ResumeFallbackBestAnswers.INTERROGATION)
                    .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
        }
    }
}
