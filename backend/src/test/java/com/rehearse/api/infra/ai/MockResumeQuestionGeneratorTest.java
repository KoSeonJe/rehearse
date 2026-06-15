package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MockResumeQuestionGenerator — API 키 부재 환경 fallback 동작")
class MockResumeQuestionGeneratorTest {

    @Test
    @DisplayName("Mock 단독 호출 시 opener/main 개수만큼 mock 질문을 반환한다")
    void generate_returnsMockQuestions() {
        MockResumeQuestionGenerator mock = new MockResumeQuestionGenerator();

        GeneratedResumeQuestions result = mock.generate(
                new byte[]{1, 2, 3}, 1, 3, Position.BACKEND, TechStack.JAVA_SPRING);

        assertThat(result.openers()).hasSize(1);
        assertThat(result.mains()).hasSize(3);
        assertThat(result.openers().get(0).question()).contains("[Mock]");
    }
}
