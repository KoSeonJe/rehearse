package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionDetailResponse - enum 단일 출처 환원")
class QuestionDetailResponseTest {

    @Nested
    @DisplayName("referenceType 환원")
    class ReferenceTypeMapping {

        @Test
        @DisplayName("TECH_MAIN + CS 카테고리 → MODEL_ANSWER")
        void techMain_resolvesModelAnswer() {
            Question question = build(QuestionType.TECH_MAIN);

            QuestionDetailResponse response =
                    QuestionDetailResponse.from(question, QuestionSetCategory.CS_FUNDAMENTAL);

            assertThat(response.getReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
        }

        @Test
        @DisplayName("BEHAVIORAL_MAIN + BEHAVIORAL 카테고리 → GUIDE")
        void behavioralMain_resolvesGuide() {
            Question question = build(QuestionType.BEHAVIORAL_MAIN);

            QuestionDetailResponse response =
                    QuestionDetailResponse.from(question, QuestionSetCategory.BEHAVIORAL);

            assertThat(response.getReferenceType()).isEqualTo(ReferenceType.GUIDE);
        }

        @Test
        @DisplayName("RESUME_INTERROGATION → GUIDE (enum 매핑 직사용)")
        void resumeInterrogation_resolvesGuide() {
            Question question = build(QuestionType.RESUME_INTERROGATION);

            QuestionDetailResponse response =
                    QuestionDetailResponse.from(question, QuestionSetCategory.RESUME_BASED);

            assertThat(response.getReferenceType()).isEqualTo(ReferenceType.GUIDE);
        }

        @Test
        @DisplayName("MAIN sentinel + BEHAVIORAL 카테고리 → GUIDE 폴백")
        void mainSentinel_behavioralCategory_fallsBackGuide() {
            Question question = build(QuestionType.MAIN);

            QuestionDetailResponse response =
                    QuestionDetailResponse.from(question, QuestionSetCategory.BEHAVIORAL);

            assertThat(response.getReferenceType()).isEqualTo(ReferenceType.GUIDE);
        }

        @Test
        @DisplayName("MAIN sentinel + CS 카테고리 → MODEL_ANSWER 폴백")
        void mainSentinel_csCategory_fallsBackModelAnswer() {
            Question question = build(QuestionType.MAIN);

            QuestionDetailResponse response =
                    QuestionDetailResponse.from(question, QuestionSetCategory.CS_FUNDAMENTAL);

            assertThat(response.getReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
        }
    }

    @Test
    @DisplayName("기본 필드 (id / questionType / questionText / orderIndex) 매핑")
    void from_mapsCoreFields() {
        Question question = build(QuestionType.TECH_MAIN);

        QuestionDetailResponse response =
                QuestionDetailResponse.from(question, QuestionSetCategory.CS_FUNDAMENTAL);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getQuestionType()).isEqualTo(QuestionType.TECH_MAIN);
        assertThat(response.getQuestionText()).isEqualTo("질문 텍스트");
        assertThat(response.getOrderIndex()).isZero();
    }

    private Question build(QuestionType type) {
        Question question = Question.builder()
                .questionType(type)
                .questionText("질문 텍스트")
                .modelAnswer("모범답변")
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(question, "id", 10L);
        return question;
    }
}
