package com.rehearse.api.domain.feedback.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimestampFeedbackMapper")
class TimestampFeedbackMapperTest {

    private final TimestampFeedbackMapper mapper = new TimestampFeedbackMapper(new ObjectMapper());

    @Nested
    @DisplayName("toEntity 메서드")
    class ToEntity {

        @Test
        @DisplayName("TimestampFeedbackItem을 TimestampFeedback 엔티티로 변환한다")
        void toEntity_validItem_createsEntity() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("질문입니다")
                    .orderIndex(0)
                    .build();

            SaveFeedbackRequest.TimestampFeedbackItem item = createTimestampFeedbackItem(
                    1000L, 5000L, "답변 내용", 2, "GOOD", "AVERAGE", "적절", List.of("음", "어"));

            // when
            TimestampFeedback result = mapper.toEntity(item, question);

            // then
            assertThat(result.getQuestion()).isSameAs(question);
            assertThat(result.getStartMs()).isEqualTo(1000L);
            assertThat(result.getEndMs()).isEqualTo(5000L);
            assertThat(result.getTranscript()).isEqualTo("답변 내용");
            assertThat(result.getFillerWordCount()).isEqualTo(2);
            assertThat(result.getEyeContactLevel()).isEqualTo("GOOD");
            assertThat(result.isAnalyzed()).isTrue();
            assertThat(result.getFillerWords()).isEqualTo("[\"음\",\"어\"]");
        }

        @Test
        @DisplayName("question이 null이어도 정상 변환된다")
        void toEntity_nullQuestion_createsEntity() {
            // given
            SaveFeedbackRequest.TimestampFeedbackItem item = createTimestampFeedbackItem(
                    0L, 3000L, null, null, null, null, null, null);

            // when
            TimestampFeedback result = mapper.toEntity(item, null);

            // then
            assertThat(result.getQuestion()).isNull();
            assertThat(result.getStartMs()).isZero();
            assertThat(result.getEndMs()).isEqualTo(3000L);
        }
    }

    @Nested
    @DisplayName("serializeCommentBlock 메서드")
    class SerializeCommentBlock {

        @Test
        @DisplayName("CommentBlock을 JSON 문자열로 직렬화한다")
        void serializeCommentBlock_validBlock_returnsJson() {
            // given
            SaveFeedbackRequest.CommentBlock block = new SaveFeedbackRequest.CommentBlock();
            ReflectionTestUtils.setField(block, "positive", "좋은 답변입니다");

            // when
            String result = mapper.serializeCommentBlock(block);

            // then
            assertThat(result).contains("좋은 답변입니다");
        }

        @Test
        @DisplayName("null이면 null을 반환한다")
        void serializeCommentBlock_null_returnsNull() {
            // when & then
            assertThat(mapper.serializeCommentBlock(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toJson 메서드")
    class ToJson {

        @Test
        @DisplayName("List<String>을 JSON 배열 문자열로 변환한다")
        void toJson_validList_returnsJsonArray() {
            // when
            String result = mapper.toJson(List.of("음", "어"));

            // then
            assertThat(result).isEqualTo("[\"음\",\"어\"]");
        }

        @Test
        @DisplayName("null이면 null을 반환한다")
        void toJson_null_returnsNull() {
            // when & then
            assertThat(mapper.toJson(null)).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private SaveFeedbackRequest.TimestampFeedbackItem createTimestampFeedbackItem(
            Long startMs, Long endMs, String transcript, Integer fillerWordCount,
            String eyeContactLevel, String postureLevel, String speechPace,
            List<String> fillerWords) {

        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "startMs", startMs);
        ReflectionTestUtils.setField(item, "endMs", endMs);
        ReflectionTestUtils.setField(item, "transcript", transcript);
        ReflectionTestUtils.setField(item, "fillerWordCount", fillerWordCount);
        ReflectionTestUtils.setField(item, "eyeContactLevel", eyeContactLevel);
        ReflectionTestUtils.setField(item, "postureLevel", postureLevel);
        ReflectionTestUtils.setField(item, "speechPace", speechPace);
        ReflectionTestUtils.setField(item, "fillerWords", fillerWords);
        return item;
    }
}
