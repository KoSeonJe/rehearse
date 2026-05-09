package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterrogationResult.withBestAnswer - 폴백 적용 시 bestAnswer 만 교체")
class InterrogationResultTest {

    @Test
    @DisplayName("withBestAnswer 호출 시 bestAnswer 만 교체되고 다른 필드 / next_action 도우미는 보존된다")
    void withBestAnswer_replacesOnlyModelAnswer_preservesOtherFields() {
        InterrogationResult original = new InterrogationResult(
                "구체적 구현 방식을 설명해주세요",
                "구체적 구현 방식을 설명해 주세요",
                "L2 진입",
                "LEVEL_UP",
                2,
                "원래 가이드"
        );

        InterrogationResult replaced = original.withBestAnswer("폴백 가이드");

        assertThat(replaced.bestAnswer()).isEqualTo("폴백 가이드");
        assertThat(replaced.question()).isEqualTo(original.question());
        assertThat(replaced.ttsQuestion()).isEqualTo(original.ttsQuestion());
        assertThat(replaced.reason()).isEqualTo(original.reason());
        assertThat(replaced.nextAction()).isEqualTo(original.nextAction());
        assertThat(replaced.nextLevel()).isEqualTo(original.nextLevel());
        assertThat(replaced.isLevelUp()).isTrue();
        assertThat(replaced.isLevelStay()).isFalse();
        assertThat(replaced.isChainSwitch()).isFalse();
    }
}
