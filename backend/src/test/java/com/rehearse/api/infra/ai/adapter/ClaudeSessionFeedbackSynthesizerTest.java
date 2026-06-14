package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.infra.ai.client.ClaudeSessionFeedbackSynthesizerClient;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.infra.ai.prompt.SessionFeedbackSynthesizerPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClaudeSessionFeedbackSynthesizer — system prompt 에 JSON 강제 지시문을 prepend 한다")
class ClaudeSessionFeedbackSynthesizerTest {

    private static final String VALID_JSON = """
            {
              "overall": {
                "level_assessment": "주니어 기대치 충족",
                "narrative": "전반적으로 안정적입니다.",
                "coverage": "all turns scored"
              },
              "strengths": [
                {"observation": "1-1 답변에서 요구사항 분해", "why_matters": "구조적 사고"}
              ],
              "gaps": [
                {"observation": "2-1 답변에서 트레이드오프 비교 부족", "concrete_action": "비교표 작성"}
              ],
              "delivery": null,
              "week_plan": [
                {"priority": 1, "topic": "트레이드오프 비교", "resources": ["공식 문서"], "practice": "비교표 1장 작성"}
              ]
            }
            """;

    private ClaudeSessionFeedbackSynthesizerClient client;
    private SessionFeedbackSynthesizerPromptBuilder promptBuilder;
    private SessionFeedbackParser parser;
    private ClaudeSessionFeedbackSynthesizer adapter;

    @BeforeEach
    void setUp() {
        client = mock(ClaudeSessionFeedbackSynthesizerClient.class);
        promptBuilder = mock(SessionFeedbackSynthesizerPromptBuilder.class);
        parser = new SessionFeedbackParser(new ObjectMapper(), new com.rehearse.api.infra.ai.AiResponseParser(new ObjectMapper()));
        adapter = new ClaudeSessionFeedbackSynthesizer(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("system prompt 앞에 JSON 강제 지시문이 prepend 된다")
    void synthesize_prependsJsonInstructionToSystemPrompt() {
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("base-sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        adapter.synthesize(sampleInput());

        ArgumentCaptor<String> sysCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(sysCaptor.capture(), any());
        String captured = sysCaptor.getValue();
        assertThat(captured).contains("JSON");
        assertThat(captured).contains("base-sys");
        assertThat(captured.indexOf("JSON")).isLessThan(captured.indexOf("base-sys"));
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedSessionFeedback 으로 매핑한다")
    void synthesize_mapsValidJson() {
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedSessionFeedback result = adapter.synthesize(sampleInput());

        assertThat(result.overall().narrative()).contains("안정적");
        assertThat(result.strengths()).hasSize(1);
    }

    @Test
    @DisplayName("파싱 실패 시 1회 재시도 후 성공하면 결과를 반환한다")
    void synthesize_retryOnParseFailure() {
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any()))
                .thenReturn("invalid")
                .thenReturn(VALID_JSON);

        GeneratedSessionFeedback result = adapter.synthesize(sampleInput());

        assertThat(result.overall().narrative()).contains("안정적");
        verify(client, times(2)).call(any(), any());
    }

    @Test
    @DisplayName("재시도까지 모두 파싱 실패하면 SessionFeedbackParseException 이 던져진다")
    void synthesize_retryFailureThrows() {
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any()))
                .thenReturn("invalid-1")
                .thenReturn("invalid-2");

        assertThatThrownBy(() -> adapter.synthesize(sampleInput()))
                .isInstanceOf(SessionFeedbackParseException.class);
        verify(client, times(2)).call(any(), any());
    }

    private SessionFeedbackInput sampleInput() {
        return new SessionFeedbackInput(
                new SessionFeedbackInput.SessionMetadata(
                        1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 2, 30),
                Collections.emptyList(),
                null, null,
                "all turns scored",
                InterviewLevel.MID
        );
    }
}
