package com.rehearse.api.domain.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import com.rehearse.api.infra.ai.prompt.AudioTurnAnalyzerPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioTurnAnalyzer - 통합 분석 (audio chat + L1 FN guard + fallback 위임)")
class AudioTurnAnalyzerTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private InterviewRuntimeStateCache runtimeStateStore;

    @Mock
    private TextFallbackTurnAnalyzer textFallbackTurnAnalyzer;

    @Mock
    private AiCallMetrics aiCallMetrics;

    private AudioTurnAnalyzer audioTurnAnalyzer;

    private static final MockMultipartFile AUDIO =
            new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        AudioTurnAnalyzerPromptBuilder promptBuilder = new AudioTurnAnalyzerPromptBuilder();
        ReflectionTestUtils.invokeMethod(promptBuilder, "init");
        AiResponseParser parser = new AiResponseParser(
                new ObjectMapper(),
                new com.rehearse.api.infra.ai.SchemaExampleRegistry(),
                aiCallMetrics);

        audioTurnAnalyzer = new AudioTurnAnalyzer(
                aiClient, parser, promptBuilder, runtimeStateStore,
                textFallbackTurnAnalyzer, aiCallMetrics);

        lenient().doAnswer(invocation -> null)
                .when(runtimeStateStore)
                .update(any(), any());
    }

    private static ChatResponse jsonResponse(String json) {
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini-audio-preview", false, false);
    }

    @Test
    @DisplayName("ANSWER intent + 유효 분석 → TurnAnalysisResult 그대로 반환 + 캐시 기록")
    void analyze_answerIntent_returnsParsedResult() {
        String json = """
                {
                  "answer_text": "JVM 힙은 Young/Old 세대로 분리됩니다.",
                  "intent": {"type":"ANSWER", "confidence":0.95, "reasoning":"GC 답변"},
                  "answer_analysis": {
                    "claims": [{"text":"Young/Old 분리", "depth_score":3, "evidence_strength":"WEAK", "topic_tag":"gc"}],
                    "missing_perspectives": ["TRADEOFF"],
                    "unstated_assumptions": [],
                    "answer_quality": 3,
                    "recommended_next_action": "DEEP_DIVE"
                  }
                }
                """;
        given(aiClient.chatWithAudio(any(ChatRequest.class), any())).willReturn(jsonResponse(json));

        TurnAnalysisResult result = audioTurnAnalyzer.analyze(
                1L, 50L, AUDIO, "JVM GC 설명", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty());

        assertThat(result.answerText()).isEqualTo("JVM 힙은 Young/Old 세대로 분리됩니다.");
        assertThat(result.intent().type()).isEqualTo(IntentType.ANSWER);
        assertThat(result.intent().confidence()).isEqualTo(0.95);
        assertThat(result.answerAnalysis().turnId()).isEqualTo(50L);
        assertThat(result.answerAnalysis().recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
        then(runtimeStateStore).should().update(any(), any());
    }

    @Test
    @DisplayName("L1 FN 가드: ANSWER + claims=[] + quality<=1 → recommended_next_action 을 CLARIFICATION 으로 override")
    void analyze_l1FnGuard_overridesToClarification() {
        String json = """
                {
                  "answer_text": "음... 잘 모르겠어요.",
                  "intent": {"type":"ANSWER", "confidence":0.55, "reasoning":"답변 시도 있으나 내용 없음"},
                  "answer_analysis": {
                    "claims": [],
                    "missing_perspectives": ["TRADEOFF"],
                    "unstated_assumptions": [],
                    "answer_quality": 1,
                    "recommended_next_action": "DEEP_DIVE"
                  }
                }
                """;
        given(aiClient.chatWithAudio(any(ChatRequest.class), any())).willReturn(jsonResponse(json));

        TurnAnalysisResult result = audioTurnAnalyzer.analyze(
                1L, 50L, AUDIO, "TCP 3-way handshake", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty());

        assertThat(result.answerAnalysis().recommendedNextAction()).isEqualTo(RecommendedNextAction.CLARIFICATION);
    }

    @Test
    @DisplayName("intent != ANSWER (OFF_TOPIC) → L1 FN 가드 미적용, 원본 분석 유지")
    void analyze_offTopicIntent_skipsGuard() {
        String json = """
                {
                  "answer_text": "시간 얼마나 남았어요?",
                  "intent": {"type":"OFF_TOPIC", "confidence":0.99, "reasoning":"메타 발화"},
                  "answer_analysis": {
                    "claims": [],
                    "missing_perspectives": [],
                    "unstated_assumptions": [],
                    "answer_quality": 1,
                    "recommended_next_action": "CLARIFICATION"
                  }
                }
                """;
        given(aiClient.chatWithAudio(any(ChatRequest.class), any())).willReturn(jsonResponse(json));

        TurnAnalysisResult result = audioTurnAnalyzer.analyze(
                1L, 50L, AUDIO, "HashMap 충돌", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty());

        assertThat(result.intent().type()).isEqualTo(IntentType.OFF_TOPIC);
        assertThat(result.answerAnalysis().recommendedNextAction()).isEqualTo(RecommendedNextAction.CLARIFICATION);
    }

    @Test
    @DisplayName("audio chat 인프라 오류 → TextFallback 위임 + 카운터 증가")
    void analyze_audioChatFailed_delegatesToTextFallback() {
        willThrow(new AudioChatFallbackRequiredException("audio chat 인프라 오류"))
                .given(aiClient).chatWithAudio(any(ChatRequest.class), any());

        TurnAnalysisResult fallback = new TurnAnalysisResult(
                "페이징 설명",
                IntentResult.of(IntentType.ANSWER, 0.9, "fallback"),
                new AnswerAnalysis(50L, List.of(), List.of(), List.of(), 2, RecommendedNextAction.DEEP_DIVE));
        given(textFallbackTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                .willReturn(fallback);

        TurnAnalysisResult result = audioTurnAnalyzer.analyze(
                1L, 50L, AUDIO, "페이징?", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty());

        assertThat(result.answerText()).isEqualTo("페이징 설명");
        assertThat(result.intent().type()).isEqualTo(IntentType.ANSWER);
        then(textFallbackTurnAnalyzer).should().analyze(any(), any(), any(), any(), any(), any());
        then(aiCallMetrics).should().incrementFollowUpSkip("audio_chat_fallback_to_stt");
        // commit() 은 fallback 경로에서 bypass 되어야 함 — runtimeStateStore.update 미호출
        then(runtimeStateStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PARSE_FAILED 같은 비-인프라 오류는 fallback 트리거 X — 그대로 전파")
    void analyze_parseFailed_doesNotTriggerFallback() {
        willThrow(new BusinessException(AiErrorCode.PARSE_FAILED))
                .given(aiClient).chatWithAudio(any(ChatRequest.class), any());

        assertThatThrownBy(() -> audioTurnAnalyzer.analyze(
                1L, 50L, AUDIO, "질문", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("AI_005"));
        then(textFallbackTurnAnalyzer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("audio file null → INTERVIEW_006")
    void analyze_nullAudio_throwsInterview006() {
        assertThatThrownBy(() -> audioTurnAnalyzer.analyze(
                1L, 50L, null, "질문", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("INTERVIEW_006"));
    }

    @Test
    @DisplayName("interviewId null → IllegalArgumentException")
    void analyze_nullInterviewId_throws() {
        assertThatThrownBy(() -> audioTurnAnalyzer.analyze(
                null, 50L, AUDIO, "질문", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
