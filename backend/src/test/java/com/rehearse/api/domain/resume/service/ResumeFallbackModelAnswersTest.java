package com.rehearse.api.domain.resume.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeFallbackModelAnswers - 분량 / 톤 / 가이드 톤 부재 / 구조 단서 가드")
class ResumeFallbackModelAnswersTest {

    @Nested
    @DisplayName("분량 검증 - 한국어 200~300자 범위")
    class LengthRange {

        @Test
        @DisplayName("opener_fallback_length_within_200_to_300_chars — OPENER 상수 길이 200~300자 범위")
        void opener_fallback_length_within_200_to_300_chars() {
            assertThat(ResumeFallbackModelAnswers.OPENER.length()).isBetween(200, 300);
        }

        @Test
        @DisplayName("playground_fallback_length_within_200_to_300_chars — PLAYGROUND 상수 길이 200~300자 범위")
        void playground_fallback_length_within_200_to_300_chars() {
            assertThat(ResumeFallbackModelAnswers.PLAYGROUND.length()).isBetween(200, 300);
        }

        @Test
        @DisplayName("interrogation_fallback_length_within_200_to_300_chars — INTERROGATION 상수 길이 200~300자 범위")
        void interrogation_fallback_length_within_200_to_300_chars() {
            assertThat(ResumeFallbackModelAnswers.INTERROGATION.length()).isBetween(200, 300);
        }
    }

    @Nested
    @DisplayName("톤 검증 - 1인칭 답변 예시 어미 1+ 매치")
    class FirstPersonTone {

        @Test
        @DisplayName("all_fallbacks_use_first_person_answer_tone — 3 상수 모두 1인칭 답변 예시 어미 1+ 매치")
        void all_fallbacks_use_first_person_answer_tone() {
            assertThat(ResumeFallbackModelAnswers.OPENER)
                    .containsAnyOf("했습니다", "경험이 있습니다", "을 진행했습니다", "을 적용했습니다", "을 측정했습니다");
            assertThat(ResumeFallbackModelAnswers.PLAYGROUND)
                    .containsAnyOf("했습니다", "경험이 있습니다", "을 진행했습니다", "을 적용했습니다", "을 측정했습니다");
            assertThat(ResumeFallbackModelAnswers.INTERROGATION)
                    .containsAnyOf("했습니다", "경험이 있습니다", "을 진행했습니다", "을 적용했습니다", "을 측정했습니다");
        }
    }

    @Nested
    @DisplayName("가이드 톤 부재 - 금지 어구 0건")
    class NoGuideTone {

        @Test
        @DisplayName("all_fallbacks_forbid_guide_tone_phrases — 3 상수 모두 가이드 톤 어구 0건")
        void all_fallbacks_forbid_guide_tone_phrases() {
            assertThat(ResumeFallbackModelAnswers.OPENER)
                    .doesNotContain("해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요");
            assertThat(ResumeFallbackModelAnswers.PLAYGROUND)
                    .doesNotContain("해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요");
            assertThat(ResumeFallbackModelAnswers.INTERROGATION)
                    .doesNotContain("해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요");
        }
    }

    @Nested
    @DisplayName("구조 단서 - STAR / 결정 근거 / 트레이드오프 / 학습 1+ 매치")
    class StructureSignal {

        @Test
        @DisplayName("all_fallbacks_include_structure_signal — 3 상수 모두 구조 단서 어휘 1+ 매치")
        void all_fallbacks_include_structure_signal() {
            assertThat(ResumeFallbackModelAnswers.OPENER)
                    .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
            assertThat(ResumeFallbackModelAnswers.PLAYGROUND)
                    .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
            assertThat(ResumeFallbackModelAnswers.INTERROGATION)
                    .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
        }
    }
}
