package com.rehearse.api.infra.ai.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resume Playground 프롬프트 템플릿 - 톤 가이드 회귀 가드")
class ResumePlaygroundPromptTemplateTest {

    private static final String OPENER_PATH = "/prompts/template/resume/resume-playground-opener.txt";
    private static final String RESPONDER_PATH = "/prompts/template/resume/resume-playground-responder.txt";

    @Nested
    @DisplayName("금지 어휘 (narrow 기술 심문)")
    class ForbiddenVocabulary {

        @Test
        @DisplayName("opener_template_includes_extended_forbidden_vocabulary — opener.txt 가 확장된 금지 어휘 6개 모두 포함한다")
        void opener_template_includes_extended_forbidden_vocabulary() {
            String template = loadResource(OPENER_PATH);

            assertThat(template)
                    .contains("왜 그렇게 설계")
                    .contains("내부 원리")
                    .contains("기술적 결정")
                    .contains("트레이드오프")
                    .contains("메커니즘")
                    .contains("어떻게 동작");
        }

        @Test
        @DisplayName("responder_template_includes_extended_forbidden_vocabulary — responder.txt 가 확장된 금지 어휘 6개 모두 포함한다")
        void responder_template_includes_extended_forbidden_vocabulary() {
            String template = loadResource(RESPONDER_PATH);

            assertThat(template)
                    .contains("왜 그렇게 설계")
                    .contains("내부 원리")
                    .contains("기술적 결정")
                    .contains("트레이드오프")
                    .contains("메커니즘")
                    .contains("어떻게 동작");
        }
    }

    @Nested
    @DisplayName("의도 어휘군 헤더")
    class AllowedToneHeader {

        @Test
        @DisplayName("opener_template_includes_intro_allowed_tone_header — 허용 톤 헤더 (역할 / 흐름 / 인상 깊은 경험 / open question) 포함")
        void opener_template_includes_intro_allowed_tone_header() {
            String template = loadResource(OPENER_PATH);

            assertThat(template)
                    .contains("역할")
                    .contains("흐름")
                    .contains("인상 깊은 경험")
                    .contains("open question");
        }

        @Test
        @DisplayName("responder_template_includes_emotion_oriented_intent_line — Responder 1턴 의도 라인 (감정·서사 oriented 어휘군) 포함")
        void responder_template_includes_emotion_oriented_intent_line() {
            String template = loadResource(RESPONDER_PATH);

            assertThat(template)
                    .contains("Responder 1턴 의도")
                    .containsAnyOf("어려웠던", "기억", "인상");
        }
    }

    @Nested
    @DisplayName("폴백 / 위임 가이드 회귀 가드")
    class FallbackPreservation {

        @Test
        @DisplayName("opener_template_preserves_safe_fallback_text — 안전 폴백 문자열 (line 40) 그대로 보존")
        void opener_template_preserves_safe_fallback_text() {
            String template = loadResource(OPENER_PATH);

            assertThat(template)
                    .contains("이 프로젝트에서 가장 인상 깊었던 경험을 자유롭게 이야기해주세요.");
        }

        @Test
        @DisplayName("opener_template_preserves_open_question_delegation_guide — projectName 누락 시 위임 가이드 (line 31-32 핵심 문구) 보존")
        void opener_template_preserves_open_question_delegation_guide() {
            String template = loadResource(OPENER_PATH);

            assertThat(template)
                    .contains("가장 자신 있게 설명할 수 있는 프로젝트")
                    .contains("임의 명칭");
        }
    }

    private static String loadResource(String path) {
        try (InputStream stream = ResumePlaygroundPromptTemplateTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError(path + " 템플릿 리소스 누락");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(path + " 템플릿 로드 실패", e);
        }
    }
}
