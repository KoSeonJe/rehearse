package com.rehearse.api.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.models.service.AudioTurnAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("AudioTurnAnalysisService — port 정상 응답을 도메인 매핑, fallback 신호 시 text-only 위임")
class AudioTurnAnalysisServiceTest {

    private static final GeneratedTurnAnalysis SAMPLE_GENERATED = new GeneratedTurnAnalysis(
            new GeneratedAnswerAnalysis(
                    "전사 텍스트",
                    List.of(),
                    Map.of("depth", 1),
                    "depth",
                    List.of(),
                    RecommendedNextAction.DEEP_DIVE));

    private static final AnswerAnalysis FALLBACK_RESULT = new AnswerAnalysis(
            "전사 텍스트", List.of(), Map.of(), null, List.of(), RecommendedNextAction.CLARIFICATION);

    private AudioTurnAnalyzer audioTurnAnalyzer;
    private TextFallbackTurnAnalyzer textFallbackTurnAnalyzer;
    private AiCallMetrics aiCallMetrics;
    private AudioTurnAnalysisService service;

    @BeforeEach
    void setUp() {
        audioTurnAnalyzer = mock(AudioTurnAnalyzer.class);
        textFallbackTurnAnalyzer = mock(TextFallbackTurnAnalyzer.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        aiCallMetrics = new AiCallMetrics(registry, new ContextEngineeringMetrics(registry));
        service = new AudioTurnAnalysisService(audioTurnAnalyzer, textFallbackTurnAnalyzer, aiCallMetrics);
    }

    @Test
    @DisplayName("port 가 정상 응답 시 toDomain() 매핑 결과 반환, text fallback 미호출")
    void analyze_success_returnsDomain() {
        MultipartFile audio = audioFile();
        when(audioTurnAnalyzer.analyze(eq(audio), eq("Q"), eq(ReferenceType.MODEL_ANSWER), eq(false)))
                .thenReturn(SAMPLE_GENERATED);

        AnswerAnalysis result = service.analyze(1L, audio, "Q", ReferenceType.MODEL_ANSWER, false);

        assertThat(result.weakestDimension()).isEqualTo("depth");
        assertThat(result.recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
        verify(textFallbackTurnAnalyzer, never()).analyze(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("port 가 AudioChatFallbackRequiredException 던지면 text-only fallback 위임, 결과 그대로 반환")
    void analyze_audioFallback_delegatesToTextFallback() {
        MultipartFile audio = audioFile();
        when(audioTurnAnalyzer.analyze(any(), any(), any(), anyBoolean()))
                .thenThrow(new AudioChatFallbackRequiredException("simulated"));
        when(textFallbackTurnAnalyzer.analyze(eq(1L), eq(audio), eq("Q"), eq(ReferenceType.GUIDE), eq(false)))
                .thenReturn(FALLBACK_RESULT);

        AnswerAnalysis result = service.analyze(1L, audio, "Q", ReferenceType.GUIDE, false);

        assertThat(result).isSameAs(FALLBACK_RESULT);
        verify(textFallbackTurnAnalyzer, times(1)).analyze(eq(1L), eq(audio), eq("Q"), eq(ReferenceType.GUIDE), eq(false));
    }

    @Test
    @DisplayName("port 가 PARSE_FAILED BusinessException 을 rethrow 하면 service 에서도 그대로 전파 (text fallback 미호출)")
    void analyze_parseFailed_propagates() {
        when(audioTurnAnalyzer.analyze(any(), any(), any(), anyBoolean()))
                .thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> service.analyze(1L, audioFile(), "Q", ReferenceType.MODEL_ANSWER, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(textFallbackTurnAnalyzer, never()).analyze(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("interviewId == null 이면 IllegalArgumentException")
    void analyze_nullInterviewId_throws() {
        assertThatThrownBy(() -> service.analyze(null, audioFile(), "Q", ReferenceType.MODEL_ANSWER, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("audioFile 이 null 이거나 빈 파일이면 ANSWER_TEXT_REQUIRED")
    void analyze_emptyAudio_throws() {
        MultipartFile empty = new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[0]);

        assertThatThrownBy(() -> service.analyze(1L, empty, "Q", ReferenceType.MODEL_ANSWER, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
    }

    @Test
    @DisplayName("audio 크기 한계 (10MB) 초과 시 CLIENT_ERROR")
    void analyze_oversizedAudio_throwsClientError() {
        byte[] tooBig = new byte[(int) (10L * 1024 * 1024 + 1)];
        MultipartFile oversized = new MockMultipartFile("audio", "a.wav", "audio/wav", tooBig);

        assertThatThrownBy(() -> service.analyze(1L, oversized, "Q", ReferenceType.MODEL_ANSWER, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);
    }

    private static MultipartFile audioFile() {
        return new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[]{1, 2, 3});
    }
}
