package com.rehearse.api.domain.feedback.dto;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimestampFeedbackResponse - 코멘트형 복원")
class TimestampFeedbackResponseTest {

    @Nested
    @DisplayName("parseCommentBlock 메서드")
    class ParseCommentBlock {

        @Test
        @DisplayName("정상적인 JSON 문자열을 CommentBlock으로 파싱한다")
        void parseCommentBlock_정상_JSON() {
            String json = "{\"positive\":\"좋음\",\"negative\":\"개선\",\"suggestion\":\"이렇게\"}";

            TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(json);

            assertThat(block).isNotNull();
            assertThat(block.getPositive()).isEqualTo("좋음");
            assertThat(block.getNegative()).isEqualTo("개선");
            assertThat(block.getSuggestion()).isEqualTo("이렇게");
        }

        @Test
        @DisplayName("null 또는 빈 문자열 입력 시 null을 반환한다")
        void parseCommentBlock_null_반환() {
            assertThat(TimestampFeedbackResponse.parseCommentBlock(null)).isNull();
            assertThat(TimestampFeedbackResponse.parseCommentBlock("")).isNull();
            assertThat(TimestampFeedbackResponse.parseCommentBlock("   ")).isNull();
        }

        @Test
        @DisplayName("JSON이 아닌 레거시 raw 문자열은 positive 필드에 그대로 담긴다")
        void parseCommentBlock_legacy_raw_문자열() {
            String legacy = "✓ 잘했음\n△ 보완\n→ 이렇게";

            TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(legacy);

            assertThat(block).isNotNull();
            assertThat(block.getPositive()).isEqualTo(legacy);
            assertThat(block.getNegative()).isNull();
            assertThat(block.getSuggestion()).isNull();
        }
    }

    @Nested
    @DisplayName("from 메서드 - content/delivery 변환")
    class From {

        @Test
        @DisplayName("코멘트 컬럼을 content/delivery 구조로 변환한다")
        void from_content_delivery_변환() {
            TimestampFeedback feedback = TimestampFeedback.builder()
                    .startMs(0)
                    .endMs(1000)
                    .transcript("답변 내용")
                    .verbalComment("{\"positive\":\"논리적\",\"negative\":\"길다\",\"suggestion\":\"요약\"}")
                    .eyeContactLevel("GOOD")
                    .postureLevel("AVERAGE")
                    .expressionLabel("자연스러움")
                    .speechPace("적절")
                    .toneConfidenceLevel("HIGH")
                    .emotionLabel("자신감")
                    .fillerWords("[\"음\",\"어\"]")
                    .fillerWordCount(2)
                    .accuracyIssues("[{\"claim\":\"A\",\"correction\":\"B\"}]")
                    .coachingStructure("STAR")
                    .coachingImprovement("두괄식")
                    .overallComment("{\"positive\":\"전반적 양호\"}")
                    .isAnalyzed(true)
                    .build();

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback);

            assertThat(response.getContent().getVerbalComment().getPositive()).isEqualTo("논리적");
            assertThat(response.getContent().getAccuracyIssues()).hasSize(1);
            assertThat(response.getContent().getAccuracyIssues().get(0).getClaim()).isEqualTo("A");
            assertThat(response.getContent().getCoaching().getStructure()).isEqualTo("STAR");
            assertThat(response.getDelivery().getNonverbal().getEyeContactLevel()).isEqualTo("GOOD");
            assertThat(response.getDelivery().getVocal().getFillerWordCount()).isEqualTo(2);
            assertThat(response.getDelivery().getVocal().getSpeechPace()).isEqualTo("적절");
            assertThat(response.getOverallComment().getPositive()).isEqualTo("전반적 양호");
            assertThat(response.isAnalyzed()).isTrue();
        }

        @Test
        @DisplayName("코멘트 컬럼이 모두 null이면 빈 구조를 반환한다")
        void from_null_컬럼_빈_구조() {
            TimestampFeedback feedback = TimestampFeedback.builder()
                    .startMs(0)
                    .endMs(1000)
                    .isAnalyzed(false)
                    .build();

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback);

            assertThat(response.getContent().getVerbalComment()).isNull();
            assertThat(response.getContent().getAccuracyIssues()).isEmpty();
            assertThat(response.getDelivery().getNonverbal().getEyeContactLevel()).isNull();
            assertThat(response.getOverallComment()).isNull();
        }
    }
}
