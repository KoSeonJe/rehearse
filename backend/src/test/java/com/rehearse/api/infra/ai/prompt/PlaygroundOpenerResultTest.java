package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaygroundOpenerResult.withModelAnswer - 폴백 적용 시 modelAnswer 만 교체")
class PlaygroundOpenerResultTest {

    @Test
    @DisplayName("withModelAnswer 호출 시 modelAnswer 만 교체되고 다른 필드는 보존된다")
    void withModelAnswer_replacesOnlyModelAnswer_preservesOtherFields() {
        PlaygroundOpenerResult original = new PlaygroundOpenerResult(
                "프로젝트를 소개해주세요.",
                "프로젝트를 소개해 주세요",
                "Playground 도입부",
                "원래 가이드"
        );

        PlaygroundOpenerResult replaced = original.withModelAnswer("폴백 가이드");

        assertThat(replaced.modelAnswer()).isEqualTo("폴백 가이드");
        assertThat(replaced.question()).isEqualTo(original.question());
        assertThat(replaced.ttsQuestion()).isEqualTo(original.ttsQuestion());
        assertThat(replaced.reason()).isEqualTo(original.reason());
    }
}
