package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder.WrapUpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WrapUpResult.withModelAnswer - 폴백 적용 시 modelAnswer 만 교체")
class WrapUpResultTest {

    @Test
    @DisplayName("withModelAnswer 호출 시 modelAnswer 만 교체되고 다른 필드는 보존된다")
    void withModelAnswer_replacesOnlyModelAnswer_preservesOtherFields() {
        WrapUpResult original = new WrapUpResult(
                "마지막으로 덧붙이고 싶은 내용 있나요?",
                "마지막으로 덧붙이고 싶은 내용 있나요",
                "회고 마무리",
                true,
                false,
                "원래 가이드"
        );

        WrapUpResult replaced = original.withModelAnswer("폴백 가이드");

        assertThat(replaced.modelAnswer()).isEqualTo("폴백 가이드");
        assertThat(replaced.question()).isEqualTo(original.question());
        assertThat(replaced.ttsQuestion()).isEqualTo(original.ttsQuestion());
        assertThat(replaced.reason()).isEqualTo(original.reason());
        assertThat(replaced.isWrapUpQuestion()).isEqualTo(original.isWrapUpQuestion());
        assertThat(replaced.sessionComplete()).isEqualTo(original.sessionComplete());
    }
}
