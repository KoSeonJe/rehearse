package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult.SwitchConditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaygroundResponderResult.withModelAnswer - 폴백 적용 시 modelAnswer 만 교체")
class PlaygroundResponderResultTest {

    @Test
    @DisplayName("withModelAnswer 호출 시 modelAnswer 만 교체되고 다른 필드는 보존된다")
    void withModelAnswer_replacesOnlyModelAnswer_preservesOtherFields() {
        SwitchConditions cond = new SwitchConditions(true, false, true, false);
        PlaygroundResponderResult original = new PlaygroundResponderResult(
                "더 자세히 말씀해주세요",
                "더 자세히 말씀해 주세요",
                "Responder follow-up",
                false,
                cond,
                "원래 가이드"
        );

        PlaygroundResponderResult replaced = original.withModelAnswer("폴백 가이드");

        assertThat(replaced.modelAnswer()).isEqualTo("폴백 가이드");
        assertThat(replaced.question()).isEqualTo(original.question());
        assertThat(replaced.ttsQuestion()).isEqualTo(original.ttsQuestion());
        assertThat(replaced.reason()).isEqualTo(original.reason());
        assertThat(replaced.shouldSwitchToInterrogation()).isEqualTo(original.shouldSwitchToInterrogation());
        assertThat(replaced.switchConditionsMet()).isSameAs(original.switchConditionsMet());
    }
}
