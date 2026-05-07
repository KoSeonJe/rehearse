package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("TurnAnalysisPipeline - STT + AnswerAnalyzer 조합")
class TurnAnalysisPipelineTest {

    @Mock private AnswerAnalyzer answerAnalyzer;

    private TurnAnalysisPipeline pipeline;

    private static final Long INTERVIEW_ID = 1L;
    private static final long TURN_INDEX = 0L;
    private static final String QUESTION = "JVM 메모리 구조를 설명해주세요.";
    private static final String ANSWER = "힙, 스택, 메서드 영역으로 구성됩니다.";
    private static final List<FollowUpExchange> EMPTY_EXCHANGES = List.of();

    private static final AnswerAnalysis STUB_ANALYSIS = new AnswerAnalysis(
            TURN_INDEX, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);

    @BeforeEach
    void setUp() {
        pipeline = new TurnAnalysisPipeline(answerAnalyzer);
    }

    @Test
    @DisplayName("AnswerAnalyzer를 호출하고 결과를 포함한 TurnAnalysisResult를 반환한다")
    void analyze_invokesAnswerAnalyzerAndReturnsResult() {
        given(answerAnalyzer.analyze(anyLong(), anyLong(), anyString(), any(), anyString(), any()))
                .willReturn(STUB_ANALYSIS);

        TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

        assertThat(result.answerText()).isEqualTo(ANSWER);
        assertThat(result.answerAnalysis()).isEqualTo(STUB_ANALYSIS);
        then(answerAnalyzer).should().analyze(anyLong(), anyLong(), anyString(), any(), anyString(), any());
    }

    @Test
    @DisplayName("answerText가 TurnAnalysisResult에 그대로 유지된다")
    void analyze_answerTextIsPreservedInResult() {
        given(answerAnalyzer.analyze(any(), anyLong(), any(), any(), any(), any())).willReturn(STUB_ANALYSIS);

        TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

        assertThat(result.answerText()).isEqualTo(ANSWER);
    }
}
