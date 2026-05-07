package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.EvidenceStrength;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.SttService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextFallbackTurnAnalyzer — STT + AnswerAnalyzer 직렬 조정")
class TextFallbackTurnAnalyzerTest {

    @Mock
    private SttService sttService;

    @Mock
    private AnswerAnalyzer answerAnalyzer;

    private static final MockMultipartFile AUDIO =
            new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

    @Test
    @DisplayName("STT 결과를 AnswerAnalyzer 에 전달하고 결과를 wrap 한다")
    void analyze_invokesAnswerAnalyzer() {
        TextFallbackTurnAnalyzer fallback = new TextFallbackTurnAnalyzer(sttService, answerAnalyzer);

        given(sttService.transcribe(any())).willReturn("페이징 설명");
        AnswerAnalysis analysis = new AnswerAnalysis(
                50L, List.of(new Claim("c", 2, EvidenceStrength.WEAK, "t")),
                List.of(), List.of(), 2, RecommendedNextAction.DEEP_DIVE);
        given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                .willReturn(analysis);

        TurnAnalysisResult result = fallback.analyze(
                1L, 50L, AUDIO, "페이징?", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty());

        assertThat(result.answerText()).isEqualTo("페이징 설명");
        assertThat(result.answerAnalysis()).isSameAs(analysis);
    }

    @Test
    @DisplayName("SttService 미설정 (null) → SERVICE_UNAVAILABLE")
    void analyze_nullSttService_throwsServiceUnavailable() {
        TextFallbackTurnAnalyzer fallback = new TextFallbackTurnAnalyzer(null, answerAnalyzer);

        assertThatThrownBy(() -> fallback.analyze(
                1L, 50L, AUDIO, "질문", ReferenceType.MODEL_ANSWER, AskedPerspectives.empty()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("AI_006"));
    }
}
