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
import java.util.Arrays;

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
                    .questionType(QuestionType.TECH_MAIN)
                    .questionText("질문입니다")
                    .orderIndex(0)
                    .build();

            SaveFeedbackRequest.TimestampFeedbackItem item = createTimestampFeedbackItem(
                    1000L, 5000L, "답변 내용", 2, List.of("음", "어"));

            // when
            TimestampFeedback result = mapper.toEntity(item, question);

            // then
            assertThat(result.getQuestion()).isSameAs(question);
            assertThat(result.getStartMs()).isEqualTo(1000L);
            assertThat(result.getEndMs()).isEqualTo(5000L);
            assertThat(result.getTranscript()).isEqualTo("답변 내용");
            assertThat(result.getFillerWordCount()).isEqualTo(2);
            assertThat(result.isAnalyzed()).isTrue();
            assertThat(result.getFillerWords()).isEqualTo("[\"음\",\"어\"]");
        }

        @Test
        @DisplayName("question이 null이어도 정상 변환된다")
        void toEntity_nullQuestion_createsEntity() {
            // given
            SaveFeedbackRequest.TimestampFeedbackItem item = createTimestampFeedbackItem(
                    0L, 3000L, null, null, null);

            // when
            TimestampFeedback result = mapper.toEntity(item, null);

            // then
            assertThat(result.getQuestion()).isNull();
            assertThat(result.getStartMs()).isZero();
            assertThat(result.getEndMs()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("plan-13 legacy Lambda content fields are ignored")
        void toEntity_legacyLambdaContentFields_ignoresContent() throws Exception {
            // given
            String json = """
                    {
                      "questionSetComment": "음성 전달 분석 완료",
                      "isVerbalCompleted": true,
                      "isNonverbalCompleted": true,
                      "timestampFeedbacks": [
                        {
                          "startMs": 0,
                          "endMs": 1000,
                          "transcript": "캐시를 먼저 확인합니다",
                          "verbalComment": {"positive": "legacy"},
                          "accuracyIssues": "[{\\"claim\\":\\"x\\",\\"correction\\":\\"y\\"}]",
                          "coachingStructure": "legacy structure",
                          "coachingImprovement": "legacy improvement",
                          "fillerWordCount": 0
                        }
                      ]
                    }
                    """;
            ObjectMapper objectMapper = new ObjectMapper();
            SaveFeedbackRequest request = objectMapper.readValue(json, SaveFeedbackRequest.class);

            // when
            TimestampFeedback result = mapper.toEntity(request.getTimestampFeedbacks().getFirst(), null);

            // then
            assertThat(result.getTranscript()).isEqualTo("캐시를 먼저 확인합니다");
            assertThat(Arrays.stream(TimestampFeedback.class.getDeclaredFields())
                    .map(field -> field.getName()))
                    .doesNotContain(
                            "verbalComment",
                            "accuracyIssues",
                            "coachingStructure",
                            "coachingImprovement"
                    );
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
            List<String> fillerWords) {

        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "startMs", startMs);
        ReflectionTestUtils.setField(item, "endMs", endMs);
        ReflectionTestUtils.setField(item, "transcript", transcript);
        ReflectionTestUtils.setField(item, "fillerWordCount", fillerWordCount);
        ReflectionTestUtils.setField(item, "fillerWords", fillerWords);
        return item;
    }
}
