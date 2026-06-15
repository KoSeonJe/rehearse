package com.rehearse.api.infra.ai.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PromptTemplateLoader — 등록된 callType 만 GLOBAL_CORE + 템플릿으로 로드한다")
class PromptTemplateLoaderTest {

    private PromptTemplateLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PromptTemplateLoader();
        loader.init();
    }

    @Test
    @DisplayName("follow_up_concept callType 은 CONCEPT 개념 심화 템플릿을 로드한다")
    void system_loadsConceptTemplate() {
        String system = loader.system(PromptTemplateLoader.FOLLOW_UP_CONCEPT);

        assertThat(system)
                .contains("당신은 한국어 개발자 기술 면접 시스템의 AI 컴포넌트입니다.")
                .contains("평가 차원 (CONCEPT 5종)");
    }

    @Test
    @DisplayName("follow_up_experience callType 은 EXPERIENCE 경험 구체화 템플릿을 로드한다")
    void system_loadsExperienceTemplate() {
        String system = loader.system(PromptTemplateLoader.FOLLOW_UP_EXPERIENCE);

        assertThat(system).contains("평가 차원 (EXPERIENCE 4종)");
    }

    @Test
    @DisplayName("follow_up_resume callType 은 RESUME 이력서 정합 템플릿을 로드한다")
    void system_loadsResumeTemplate() {
        String system = loader.system(PromptTemplateLoader.FOLLOW_UP_RESUME);

        assertThat(system).contains("평가 차원 (RESUME 10종)");
    }

    @Test
    @DisplayName("제거된 follow_up_generator_v3 callType 호출 시 IllegalStateException 이 발생한다")
    void system_throws_when_removedV3CallType() {
        assertThatThrownBy(() -> loader.system("follow_up_generator_v3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("등록되지 않은 callType");
    }
}
