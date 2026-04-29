package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerResponse")
class AnswerResponseTest {

    @Test
    @DisplayName("Lambda 입력 호환을 위해 difficulty 기본값 easy를 포함")
    void from_includesDefaultDifficultyEasy() {
        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("질문")
                .modelAnswer("모범답변")
                .feedbackPerspective(FeedbackPerspective.TECHNICAL)
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(question, "id", 10L);
        QuestionAnswer answer = QuestionAnswer.builder()
                .question(question)
                .startMs(100L)
                .endMs(200L)
                .build();
        ReflectionTestUtils.setField(answer, "id", 20L);

        AnswerResponse response = AnswerResponse.from(answer);

        assertThat(response.getDifficulty()).isEqualTo("easy");
    }
}
