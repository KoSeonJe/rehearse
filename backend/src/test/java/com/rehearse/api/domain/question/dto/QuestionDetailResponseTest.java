package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionDetailResponse - 질문 상세 응답 매핑")
class QuestionDetailResponseTest {

    @Test
    @DisplayName("기본 필드 (id / questionType / questionText / bestAnswer / ttsText / orderIndex) 매핑")
    void from_mapsCoreFields() {
        Question question = build(QuestionType.TECH_MAIN);

        QuestionDetailResponse response = QuestionDetailResponse.from(question);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getQuestionType()).isEqualTo(QuestionType.TECH_MAIN);
        assertThat(response.getQuestionText()).isEqualTo("질문 텍스트");
        assertThat(response.getBestAnswer()).isEqualTo("모범답변");
        assertThat(response.getTtsText()).isEqualTo("tts 텍스트");
        assertThat(response.getOrderIndex()).isZero();
    }

    private Question build(QuestionType type) {
        Question question = Question.builder()
                .questionType(type)
                .questionText("질문 텍스트")
                .bestAnswer("모범답변")
                .ttsText("tts 텍스트")
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(question, "id", 10L);
        return question;
    }
}
