package com.rehearse.api.infra.ai.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resume Chain Interrogator 프롬프트 템플릿 - model_answer 분량 / 톤 / 레벨 깊이 가드")
class ResumeChainInterrogatorPromptTemplateTest {

    private static final String INTERROGATOR_PATH = "/prompts/template/resume/resume-chain-interrogator.txt";

    @Nested
    @DisplayName("model_answer 정의 라인")
    class ModelAnswerDefinitionLine {

        @Test
        @DisplayName("interrogator_template_defines_model_answer_length_200_to_300 — 정의가 200~300자 / 3~5줄 / 1인칭 모범답변 예시 키워드 포함")
        void interrogator_template_defines_model_answer_length_200_to_300() {
            String template = loadResource(INTERROGATOR_PATH);

            assertThat(template)
                    .contains("200~300자")
                    .contains("3~5줄")
                    .contains("1인칭 모범답변 예시")
                    .doesNotContain("50~200자");
        }

        @Test
        @DisplayName("interrogator_template_specifies_level_depth_differentiation — L1~L4 4 레벨 깊이 차등 키워드 모두 포함")
        void interrogator_template_specifies_level_depth_differentiation() {
            String template = loadResource(INTERROGATOR_PATH);

            assertThat(template)
                    .contains("L1=수행 사실")
                    .contains("L2=구체 구현")
                    .contains("L3=내부 메커니즘")
                    .contains("L4=대안 비교");
        }

        @Test
        @DisplayName("interrogator_template_requires_structure_signal — 정의에 STAR / 결정 근거 / 트레이드오프 / 학습 4 구조 단서 어휘 포함")
        void interrogator_template_requires_structure_signal() {
            String template = loadResource(INTERROGATOR_PATH);

            assertThat(template)
                    .contains("STAR")
                    .contains("Situation")
                    .contains("Task")
                    .contains("Action")
                    .contains("Result")
                    .contains("결정 근거")
                    .contains("트레이드오프")
                    .contains("학습");
        }
    }

    @Nested
    @DisplayName("model_answer 자가검증 라인")
    class ModelAnswerSelfValidationLine {

        @Test
        @DisplayName("interrogator_template_forbids_guide_tone — 자가검증이 가이드 톤 어구 금지를 명시")
        void interrogator_template_forbids_guide_tone() {
            String template = loadResource(INTERROGATOR_PATH);

            assertThat(template)
                    .contains("~해보세요")
                    .contains("~하면 좋습니다")
                    .contains("폐기 후 재작성");
        }

        @Test
        @DisplayName("interrogator_template_requires_chain_topic_mention — 자가검증이 CURRENT_CHAIN.topic / CURRENT_LEVEL 키워드를 포함")
        void interrogator_template_requires_chain_topic_mention() {
            String template = loadResource(INTERROGATOR_PATH);

            assertThat(template)
                    .contains("CURRENT_CHAIN.topic")
                    .contains("CURRENT_LEVEL");
        }
    }

    private static String loadResource(String path) {
        try (InputStream stream = ResumeChainInterrogatorPromptTemplateTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError(path + " 템플릿 리소스 누락");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(path + " 템플릿 로드 실패", e);
        }
    }
}
