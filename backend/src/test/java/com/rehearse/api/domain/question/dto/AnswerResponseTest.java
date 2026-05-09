package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerResponse")
class AnswerResponseTest {

    @Test
    @DisplayName("Lambda 입력 호환을 위해 difficulty 기본값 easy를 포함")
    void from_includesDefaultDifficultyEasy() {
        Question question = buildQuestion(QuestionType.TECH_MAIN);
        QuestionAnswer answer = buildAnswer(question);

        AnswerResponse response = AnswerResponse.from(answer, InterviewType.CS_FUNDAMENTAL);

        assertThat(response.getDifficulty()).isEqualTo("easy");
    }

    @Nested
    @DisplayName("feedbackPerspective 환원")
    class Perspective {

        @Test
        @DisplayName("TECH_MAIN + CS 카테고리 → TECHNICAL")
        void techMain_resolvesTechnical() {
            Question question = buildQuestion(QuestionType.TECH_MAIN);
            QuestionAnswer answer = buildAnswer(question);

            AnswerResponse response = AnswerResponse.from(answer, InterviewType.CS_FUNDAMENTAL);

            assertThat(response.getFeedbackPerspective()).isEqualTo("TECHNICAL");
        }

        @Test
        @DisplayName("BEHAVIORAL_MAIN + BEHAVIORAL 카테고리 → BEHAVIORAL")
        void behavioralMain_resolvesBehavioral() {
            Question question = buildQuestion(QuestionType.BEHAVIORAL_MAIN);
            QuestionAnswer answer = buildAnswer(question);

            AnswerResponse response = AnswerResponse.from(answer, InterviewType.BEHAVIORAL);

            assertThat(response.getFeedbackPerspective()).isEqualTo("BEHAVIORAL");
        }

        @Test
        @DisplayName("RESUME_INTERROGATION → TECHNICAL (enum 매핑)")
        void resumeInterrogation_resolvesTechnical() {
            Question question = buildQuestion(QuestionType.RESUME_INTERROGATION);
            QuestionAnswer answer = buildAnswer(question);

            AnswerResponse response = AnswerResponse.from(answer, InterviewType.RESUME_BASED);

            assertThat(response.getFeedbackPerspective()).isEqualTo("TECHNICAL");
        }

    }

    private Question buildQuestion(QuestionType type) {
        Question question = Question.builder()
                .questionType(type)
                .questionText("질문")
                .modelAnswer("모범답변")
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(question, "id", 10L);
        return question;
    }

    private QuestionAnswer buildAnswer(Question question) {
        QuestionAnswer answer = QuestionAnswer.builder()
                .question(question)
                .startMs(100L)
                .endMs(200L)
                .build();
        ReflectionTestUtils.setField(answer, "id", 20L);
        return answer;
    }
}
