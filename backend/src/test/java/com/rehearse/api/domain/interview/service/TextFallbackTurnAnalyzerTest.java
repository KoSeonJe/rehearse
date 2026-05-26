package com.rehearse.api.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.SttService;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("TextFallbackTurnAnalyzer — STT 전사 결과를 transcript 진실값으로 결정적 주입")
class TextFallbackTurnAnalyzerTest {

    private SttService sttService;
    private AnswerAnalyzer answerAnalyzer;
    private TextFallbackTurnAnalyzer textFallbackTurnAnalyzer;

    @BeforeEach
    void setUp() {
        sttService = mock(SttService.class);
        answerAnalyzer = mock(AnswerAnalyzer.class);
        textFallbackTurnAnalyzer = new TextFallbackTurnAnalyzer(sttService, new AnswerAnalysisService(answerAnalyzer));
    }

    @Test
    @DisplayName("LLM 이 채운 transcript 를 무시하고 STT 전사 결과로 덮어쓴다")
    void analyze_overridesTranscriptWithSttResult() {
        when(sttService.transcribe(any())).thenReturn("STT 가 복구한 답변 텍스트");
        when(answerAnalyzer.analyze(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(generatedWith("LLM 이 임의로 만든 transcript"));

        AnswerAnalysis result = textFallbackTurnAnalyzer.analyze(
                1L, audioFile(), "Q", ReferenceType.MODEL_ANSWER, false);

        assertThat(result.transcript()).isEqualTo("STT 가 복구한 답변 텍스트");
        assertThat(result.weakestDimension()).isEqualTo("depth");
    }

    @Test
    @DisplayName("STT 가 빈 문자열 (진짜 무응답) 반환 시 transcript 는 blank 유지 — RubricScoring blank-guard 가 NOT_EVALUABLE 처리")
    void analyze_keepsBlankTranscript_whenSttReturnsEmpty() {
        when(sttService.transcribe(any())).thenReturn("");
        when(answerAnalyzer.analyze(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(generatedWith("LLM 이 만든 가짜 transcript"));

        AnswerAnalysis result = textFallbackTurnAnalyzer.analyze(
                1L, audioFile(), "Q", ReferenceType.MODEL_ANSWER, false);

        assertThat(result.transcript()).isEmpty();
    }

    @Test
    @DisplayName("STT 서비스 미설정 시 SERVICE_UNAVAILABLE — fallback 불가")
    void analyze_throws_whenSttServiceUnavailable() {
        TextFallbackTurnAnalyzer noStt = new TextFallbackTurnAnalyzer(null, new AnswerAnalysisService(answerAnalyzer));

        assertThatThrownBy(() -> noStt.analyze(1L, audioFile(), "Q", ReferenceType.MODEL_ANSWER, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    private static GeneratedAnswerAnalysis generatedWith(String transcript) {
        return new GeneratedAnswerAnalysis(
                transcript, List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);
    }

    private static MultipartFile audioFile() {
        return new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[]{1, 2, 3});
    }
}
