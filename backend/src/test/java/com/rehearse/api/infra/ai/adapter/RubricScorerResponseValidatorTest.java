package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.infra.ai.adapter.RubricScorerResponseValidator.ValidationResult;
import com.rehearse.api.infra.ai.adapter.RubricScorerResponseValidator.Violation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RubricScorerResponseValidator")
class RubricScorerResponseValidatorTest {

    private RubricScorerResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RubricScorerResponseValidator();
    }

    @Nested
    @DisplayName("observation 한국어 음절 검증")
    class KoreanObservation {

        @Test
        @DisplayName("영문 단독 observation 은 NON_KOREAN_OBSERVATION 위배")
        void englishOnly_violatesNonKorean() {
            ValidationResult result = validator.validate(
                    "technical_depth", 2, "Good answer with technical depth", "TPS 10000",
                    "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.NON_KOREAN_OBSERVATION);
            assertThat(result.field()).isEqualTo("observation");
        }

        @Test
        @DisplayName("영문+한국어 1음절 이상 동반 observation 은 통과")
        void englishWithKoreanSyllable_passes() {
            ValidationResult result = validator.validate(
                    "technical_depth", 2, "TPS 수치 언급으로 구체적 답변", "TPS 10000",
                    "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("자모 단독 (ㄱㅏ 등) observation 은 NON_KOREAN_OBSERVATION 위배")
        void jamoOnly_violatesNonKorean() {
            ValidationResult result = validator.validate(
                    "technical_depth", 2, "ㄱㄴㄷ ㅏㅑㅓ", "TPS 10000",
                    "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.NON_KOREAN_OBSERVATION);
        }
    }

    @Nested
    @DisplayName("evidence_quote substring 검증")
    class EvidenceSubstring {

        @Test
        @DisplayName("evidence_quote 가 루브릭 정의문 substring 이면 EVIDENCE_NOT_IN_USER_ANSWER 위배")
        void rubricAnchorQuote_violatesNotInUserAnswer() {
            ValidationResult result = validator.validate(
                    "experience_concreteness", 1, "구체성 부족", "팀에서 했어요 수준",
                    "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.EVIDENCE_NOT_IN_USER_ANSWER);
            assertThat(result.field()).isEqualTo("evidence_quote");
        }

        @Test
        @DisplayName("evidence_quote 가 userAnswer substring (whitespace 정규화 후) 이면 통과")
        void substringAfterWhitespaceNormalization_passes() {
            ValidationResult result = validator.validate(
                    "technical_depth", 3, "구체적 수치 인용",
                    "TPS   10000  을\n달성",
                    "저는 작년에 TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isTrue();
        }
    }

    @Nested
    @DisplayName("score 범위 검증")
    class ScoreRange {

        @Test
        @DisplayName("score=0 은 INVALID_SCORE 위배")
        void scoreZero_violatesInvalid() {
            ValidationResult result = validator.validate(
                    "technical_depth", 0, "구체적 답변", "TPS 10000",
                    "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.INVALID_SCORE);
            assertThat(result.field()).isEqualTo("score");
        }

        @Test
        @DisplayName("score=4 는 INVALID_SCORE 위배")
        void scoreFour_violatesInvalid() {
            ValidationResult result = validator.validate(
                    "technical_depth", 4, "구체적 답변", "TPS 10000",
                    "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.INVALID_SCORE);
        }
    }

    @Nested
    @DisplayName("observation null/blank 검증")
    class ObservationBlank {

        @Test
        @DisplayName("observation null 은 MISSING_OBSERVATION 위배")
        void nullObservation_violatesMissing() {
            ValidationResult result = validator.validate(
                    "technical_depth", 2, null, "TPS 10000", "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.MISSING_OBSERVATION);
            assertThat(result.field()).isEqualTo("observation");
        }

        @Test
        @DisplayName("observation 빈 문자열 은 MISSING_OBSERVATION 위배")
        void emptyObservation_violatesMissing() {
            ValidationResult result = validator.validate(
                    "technical_depth", 2, "   ", "TPS 10000", "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.MISSING_OBSERVATION);
        }
    }

    @Nested
    @DisplayName("evidence_quote null/blank 검증")
    class EvidenceMissing {

        @Test
        @DisplayName("evidence_quote null 은 MISSING_EVIDENCE 위배")
        void nullEvidence_violatesMissing() {
            ValidationResult result = validator.validate(
                    "technical_depth", 2, "구체적 답변", null, "TPS 10000 을 달성했습니다");

            assertThat(result.valid()).isFalse();
            assertThat(result.violation()).isEqualTo(Violation.MISSING_EVIDENCE);
            assertThat(result.field()).isEqualTo("evidence_quote");
        }
    }
}
