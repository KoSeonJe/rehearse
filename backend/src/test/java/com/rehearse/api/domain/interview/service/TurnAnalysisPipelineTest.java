package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("TurnAnalysisPipeline - STT + IntentClassifier + AnswerAnalyzer 조합")
class TurnAnalysisPipelineTest {

    @Mock private IntentClassifier intentClassifier;
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
        pipeline = new TurnAnalysisPipeline(intentClassifier, answerAnalyzer);
    }

    @Nested
    @DisplayName("ANSWER intent")
    class WhenAnswerIntent {

        @Test
        @DisplayName("ANSWER 분류 시 AnswerAnalyzer를 호출하고 결과를 포함한 TurnAnalysisResult를 반환한다")
        void analyze_answer_invokesAnswerAnalyzerAndReturnsResult() {
            IntentResult answerIntent = IntentResult.of(IntentType.ANSWER, 0.9, "명확한 답변");
            given(intentClassifier.classify(eq(QUESTION), eq(ANSWER), eq(EMPTY_EXCHANGES)))
                    .willReturn(answerIntent);
            given(answerAnalyzer.analyze(anyLong(), anyLong(), anyString(), any(), anyString(), any()))
                    .willReturn(STUB_ANALYSIS);

            TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

            assertThat(result.answerText()).isEqualTo(ANSWER);
            assertThat(result.intent().type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.answerAnalysis()).isEqualTo(STUB_ANALYSIS);
            then(answerAnalyzer).should().analyze(anyLong(), anyLong(), anyString(), any(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("ANSWER가 아닌 intent")
    class WhenNonAnswerIntent {

        @Test
        @DisplayName("CLARIFY_REQUEST 분류 시 AnswerAnalyzer를 호출하지 않고 empty AnswerAnalysis를 반환한다")
        void analyze_clarify_skipsAnswerAnalyzer() {
            IntentResult clarifyIntent = IntentResult.of(IntentType.CLARIFY_REQUEST, 0.85, "질문 이해 요청");
            given(intentClassifier.classify(eq(QUESTION), eq(ANSWER), eq(EMPTY_EXCHANGES)))
                    .willReturn(clarifyIntent);

            TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

            assertThat(result.intent().type()).isEqualTo(IntentType.CLARIFY_REQUEST);
            assertThat(result.answerAnalysis().claims()).isEmpty();
            assertThat(result.answerAnalysis().answerQuality()).isEqualTo(1);
            then(answerAnalyzer).should(never()).analyze(any(), anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("OFF_TOPIC 분류 시 AnswerAnalyzer를 호출하지 않는다")
        void analyze_offTopic_skipsAnswerAnalyzer() {
            IntentResult offTopicIntent = IntentResult.of(IntentType.OFF_TOPIC, 0.9, "질문 범위 밖 응답");
            given(intentClassifier.classify(eq(QUESTION), eq(ANSWER), eq(EMPTY_EXCHANGES)))
                    .willReturn(offTopicIntent);

            TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

            assertThat(result.intent().type()).isEqualTo(IntentType.OFF_TOPIC);
            then(answerAnalyzer).should(never()).analyze(any(), anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GIVE_UP 분류 시 AnswerAnalyzer를 호출하지 않는다")
        void analyze_giveUp_skipsAnswerAnalyzer() {
            IntentResult giveUpIntent = IntentResult.of(IntentType.GIVE_UP, 0.95, "포기 선언");
            given(intentClassifier.classify(eq(QUESTION), eq(ANSWER), eq(EMPTY_EXCHANGES)))
                    .willReturn(giveUpIntent);

            TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

            assertThat(result.intent().type()).isEqualTo(IntentType.GIVE_UP);
            then(answerAnalyzer).should(never()).analyze(any(), anyLong(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("answerText가 TurnAnalysisResult에 그대로 유지된다")
    void analyze_answerTextIsPreservedInResult() {
        IntentResult answerIntent = IntentResult.of(IntentType.ANSWER, 0.9, "명확한 답변");
        given(intentClassifier.classify(any(), any(), any())).willReturn(answerIntent);
        given(answerAnalyzer.analyze(any(), anyLong(), any(), any(), any(), any())).willReturn(STUB_ANALYSIS);

        TurnAnalysisResult result = pipeline.analyze(INTERVIEW_ID, TURN_INDEX, QUESTION, ANSWER, EMPTY_EXCHANGES);

        assertThat(result.answerText()).isEqualTo(ANSWER);
    }
}
