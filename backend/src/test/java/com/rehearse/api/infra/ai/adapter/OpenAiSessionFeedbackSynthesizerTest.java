package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.infra.ai.client.OpenAiSessionFeedbackSynthesizerClient;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.infra.ai.prompt.SessionFeedbackSynthesizerPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiSessionFeedbackSynthesizer — PromptBuilder 결과를 Client.call 에 전달하고 응답을 파싱한다")
class OpenAiSessionFeedbackSynthesizerTest {

    private static final String VALID_JSON = """
            {
              "overall": {
                "level_assessment": "주니어 기대치 충족",
                "narrative": "CS 개념에서는 안정적이지만 경험 질문에서는 보강이 필요합니다.",
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

    private OpenAiSessionFeedbackSynthesizerClient client;
    private SessionFeedbackSynthesizerPromptBuilder promptBuilder;
    private SessionFeedbackParser parser;
    private OpenAiSessionFeedbackSynthesizer adapter;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiSessionFeedbackSynthesizerClient.class);
        promptBuilder = mock(SessionFeedbackSynthesizerPromptBuilder.class);
        parser = new SessionFeedbackParser(new ObjectMapper());
        adapter = new OpenAiSessionFeedbackSynthesizer(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("PromptBuilder.build 결과 (system + user) 를 Client.call 에 그대로 전달한다")
    void synthesize_passesPromptPairToClient() {
        SessionFeedbackInput input = sampleInput();
        when(promptBuilder.build(eq(input)))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys-p", "usr-p"));
        when(client.call(eq("sys-p"), eq("usr-p"))).thenReturn(VALID_JSON);

        adapter.synthesize(input);

        verify(client).call(eq("sys-p"), eq("usr-p"));
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedSessionFeedback 으로 매핑한다")
    void synthesize_mapsValidJson() {
        SessionFeedbackInput input = sampleInput();
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedSessionFeedback result = adapter.synthesize(input);

        assertThat(result.overall().narrative()).contains("CS 개념");
        assertThat(result.strengths()).hasSize(1);
        assertThat(result.gaps()).hasSize(1);
        assertThat(result.weekPlan()).hasSize(1);
    }

    @Test
    @DisplayName("파싱 실패 시 1회 재시도 후 성공하면 결과를 반환한다")
    void synthesize_retryOnParseFailure() {
        SessionFeedbackInput input = sampleInput();
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any()))
                .thenReturn("not a json")
                .thenReturn(VALID_JSON);

        GeneratedSessionFeedback result = adapter.synthesize(input);

        assertThat(result.overall().narrative()).contains("CS 개념");
        verify(client, times(2)).call(any(), any());
    }

    @Test
    @DisplayName("재시도까지 모두 파싱 실패하면 SessionFeedbackParseException 이 던져진다")
    void synthesize_retryFailureThrows() {
        SessionFeedbackInput input = sampleInput();
        when(promptBuilder.build(any()))
                .thenReturn(new SessionFeedbackSynthesizerPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any()))
                .thenReturn("invalid")
                .thenReturn("still invalid");

        assertThatThrownBy(() -> adapter.synthesize(input))
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
