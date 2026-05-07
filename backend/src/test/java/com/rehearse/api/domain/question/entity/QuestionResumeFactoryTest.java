package com.rehearse.api.domain.question.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question.resume() 팩토리 - 6-인자 시그니처 (ttsText / modelAnswer 적재)")
class QuestionResumeFactoryTest {

    @Nested
    @DisplayName("정상 생성")
    class Success {

        @Test
        @DisplayName("RESUME_OPENER 타입 + 모든 인자 채워서 호출 시 ttsText / modelAnswer 가 적재된다")
        void resume_validArgs_persistsTtsAndModelAnswer() {
            Question q = Question.resume(
                    null,
                    QuestionType.RESUME_OPENER,
                    "프로젝트를 소개해주세요.",
                    "프로젝트를 소개해 주세요",
                    "STAR 기법을 사용해 프로젝트의 핵심 경험과 결과를 소개합니다.",
                    0
            );

            assertThat(q.getQuestionText()).isEqualTo("프로젝트를 소개해주세요.");
            assertThat(q.getTtsText()).isEqualTo("프로젝트를 소개해 주세요");
            assertThat(q.getModelAnswer()).isEqualTo("STAR 기법을 사용해 프로젝트의 핵심 경험과 결과를 소개합니다.");
            assertThat(q.getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);
            assertThat(q.getOrderIndex()).isZero();
        }

        @Test
        @DisplayName("RESUME 트랙은 referenceType / feedbackPerspective 컬럼이 NULL 로 유지된다 (Option X)")
        void resume_resumeType_keepsNullColumns() {
            Question q = Question.resume(
                    null, QuestionType.RESUME_INTERROGATION,
                    "구체적인 구현 방식이 어떻게 됐나요?",
                    "구체적인 구현 방식이 어떻게 됐나요",
                    "Redis pipeline 사용 시 RTT 절감 원리 및 적용 시 주의점을 설명합니다.",
                    3
            );

            assertThat(q.getReferenceType()).isNull();
            assertThat(q.getFeedbackPerspective()).isNull();
        }

        @Test
        @DisplayName("ttsText / modelAnswer 가 null 로 들어와도 entity 생성은 성공한다 (DB 컬럼 nullable)")
        void resume_nullTtsAndModelAnswer_allowed() {
            Question q = Question.resume(
                    null, QuestionType.RESUME_PLAYGROUND, "유효한 질문",
                    null, null, 1
            );

            assertThat(q.getTtsText()).isNull();
            assertThat(q.getModelAnswer()).isNull();
        }
    }

    @Nested
    @DisplayName("입력 거부")
    class Rejection {

        @Test
        @DisplayName("MAIN 타입은 resume() 팩토리에서 거부된다")
        void resume_mainType_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, QuestionType.MAIN, "유효한 질문", "tts", "ma", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RESUME_* 타입만 허용");
        }

        @Test
        @DisplayName("FOLLOWUP 타입은 resume() 팩토리에서 거부된다")
        void resume_followupType_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, QuestionType.FOLLOWUP, "유효한 질문", "tts", "ma", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RESUME_* 타입만 허용");
        }

        @Test
        @DisplayName("questionText 가 blank 이면 IllegalArgumentException 을 던진다 (확장 인자 무관)")
        void resume_blankQuestionText_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, QuestionType.RESUME_OPENER, "  ", "tts", "ma", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText must not be blank");
        }

        @Test
        @DisplayName("questionType 이 null 이면 IllegalArgumentException 을 던진다")
        void resume_nullType_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, null, "유효한 질문", "tts", "ma", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionType must not be null");
        }
    }
}
