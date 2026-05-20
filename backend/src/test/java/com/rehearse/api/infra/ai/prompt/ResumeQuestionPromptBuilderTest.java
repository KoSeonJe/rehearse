package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeQuestionPromptBuilder — 직무 페르소나 + 5 깊이 유형 + 표층 금지 패턴 주입")
class ResumeQuestionPromptBuilderTest extends ServiceIntegrationSupport {

    @Autowired
    private ResumeQuestionPromptBuilder builder;

    @Nested
    @DisplayName("토큰 치환")
    class TokenSubstitution {

        @Test
        @DisplayName("BACKEND/JAVA_SPRING → fullPersona / evaluationPerspective / followUpDepth 텍스트가 시스템 프롬프트에 포함된다")
        void backend_persona_substitution() {
            String prompt = builder.buildSystemPrompt(Position.BACKEND, TechStack.JAVA_SPRING);

            assertThat(prompt)
                    .as("backend persona_block 의 핵심 어휘 포함")
                    .contains("백엔드 시니어 개발자 면접관");
            assertThat(prompt)
                    .as("backend evaluation_perspective 의 핵심 어휘 포함")
                    .contains("동시성 제어");
            assertThat(prompt)
                    .as("backend follow_up_depth 의 핵심 어휘 포함")
                    .contains("스레드 안전성");
            assertThat(prompt)
                    .as("토큰 슬롯이 모두 치환되어 남아있지 않다")
                    .doesNotContain("{PERSONA_BLOCK}")
                    .doesNotContain("{EVALUATION_PERSPECTIVE}")
                    .doesNotContain("{FOLLOW_UP_DEPTH}")
                    .doesNotContain("{DEPTH_GUIDE_5_TYPES}")
                    .doesNotContain("{FORBIDDEN_PATTERNS}");
        }

        @Test
        @DisplayName("FRONTEND/REACT_TS → backend 페르소나 어휘 부재 + frontend 페르소나 어휘 포함")
        void frontend_persona_substitution() {
            String prompt = builder.buildSystemPrompt(Position.FRONTEND, TechStack.REACT_TS);

            assertThat(prompt)
                    .as("frontend persona_block 의 핵심 어휘 포함")
                    .contains("프론트엔드 시니어 개발자 면접관");
            assertThat(prompt)
                    .as("backend persona 어휘 부재")
                    .doesNotContain("백엔드 시니어 개발자 면접관");
        }
    }

    @Nested
    @DisplayName("5 깊이 유형 가이드 / 표층 금지 패턴 본문")
    class DepthGuideAndForbiddenPatterns {

        @Test
        @DisplayName("출력 프롬프트에 5 깊이 유형 enum 5개 라벨이 모두 포함된다")
        void depth_guide_contains_all_5_enum_labels() {
            String prompt = builder.buildSystemPrompt(Position.BACKEND, TechStack.JAVA_SPRING);

            assertThat(prompt)
                    .contains("TRADEOFF")
                    .contains("LIMITATION")
                    .contains("QUANTITATIVE")
                    .contains("ALTERNATIVE")
                    .contains("PRINCIPLE");
        }

        @Test
        @DisplayName("출력 프롬프트에 표층 금지 패턴 본문이 포함된다")
        void forbidden_patterns_section_is_present() {
            String prompt = builder.buildSystemPrompt(Position.BACKEND, TechStack.JAVA_SPRING);

            assertThat(prompt)
                    .contains("표층 질문 금지 패턴")
                    .contains("왜 X 를 사용 / 선택 / 채택")
                    .contains("X 의 장점은 무엇인가요");
        }

        @Test
        @DisplayName("출력 프롬프트에 depth_signals 인용 가이드 + depth_type 필드 명시 룰이 포함된다")
        void output_schema_includes_depth_type_field_rule() {
            String prompt = builder.buildSystemPrompt(Position.BACKEND, TechStack.JAVA_SPRING);

            assertThat(prompt)
                    .contains("depth_signals")
                    .contains("depth_type");
        }
    }
}
