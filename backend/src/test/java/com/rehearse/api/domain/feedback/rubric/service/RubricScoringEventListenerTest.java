package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.event.AnswerAnalysisCompletedEvent;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RubricScoringEventListener — AnswerAnalysisCompletedEvent 채점 트리거")
class RubricScoringEventListenerTest {

    @Mock private RubricScorer rubricScorer;
    @Mock private QuestionScorePersister questionScorePersister;
    @Mock private InterviewFinder interviewFinder;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionSetRepository questionSetRepository;
    @Mock private AiCallMetrics aiCallMetrics;

    @InjectMocks
    private RubricScoringEventListener listener;

    private Interview interview;
    private QuestionSet questionSet;
    private Question question;

    @BeforeEach
    void setUp() {
        interview = TestFixtures.createInterview();
        ReflectionTestUtils.setField(interview, "id", 10L);
        questionSet = TestFixtures.createQuestionSet(interview);
        ReflectionTestUtils.setField(questionSet, "id", 50L);
        question = TestFixtures.createResumeQuestion(QuestionType.RESUME_OPENER);
        question.assignQuestionSet(questionSet);
        ReflectionTestUtils.setField(question, "id", 100L);

        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findById(100L)).willReturn(Optional.of(question));
        given(questionSetRepository.findById(50L)).willReturn(Optional.of(questionSet));
    }

    @Nested
    @DisplayName("채점 결과 비어있지 않으면 saveRubric 호출")
    class ScoringSuccess {

        @Test
        @DisplayName("opener 답변 (AnswerAnalysis.empty) 이벤트도 채점되어 저장된다")
        void persists_score_for_opener_answer() {
            AnswerAnalysisCompletedEvent event = AnswerAnalysisCompletedEvent.of(
                    10L, 1L, 100L, 50L,
                    AnswerAnalysis.empty(), InterviewLevel.JUNIOR);
            Map<String, DimensionScore> dims = Map.of("clarity", DimensionScore.of(2, "ok", "quote"));
            RubricScoringResult result = new RubricScoringResult("resume-v1", List.of("clarity"), dims, "L2");
            given(rubricScorer.score(eq(question), eq(questionSet), eq(interview),
                    any(AnswerAnalysis.class))).willReturn(result);

            listener.on(event);

            then(questionScorePersister).should(times(1))
                    .saveRubric(100L, 10L, "resume-v1", "L2", dims);
        }

        @Test
        @DisplayName("일반 답변 이벤트도 채점되어 저장된다")
        void persists_score_for_regular_answer() {
            AnswerAnalysis analysis = AnswerAnalysis.empty();
            AnswerAnalysisCompletedEvent event = AnswerAnalysisCompletedEvent.of(
                    10L, 1L, 100L, 50L, analysis, InterviewLevel.JUNIOR);
            Map<String, DimensionScore> dims = Map.of("clarity", DimensionScore.of(3, "good", "quote2"));
            RubricScoringResult result = new RubricScoringResult("resume-v1", List.of("clarity"), dims, null);
            given(rubricScorer.score(eq(question), eq(questionSet), eq(interview),
                    eq(analysis))).willReturn(result);

            listener.on(event);

            then(questionScorePersister).should(times(1))
                    .saveRubric(100L, 10L, "resume-v1", null, dims);
        }
    }

    @Test
    @DisplayName("RubricScore empty 이면 saveRubric 호출되지 않는다")
    void skips_save_when_scoring_result_empty() {
        AnswerAnalysisCompletedEvent event = AnswerAnalysisCompletedEvent.of(
                10L, 1L, 100L, 50L, AnswerAnalysis.empty(), InterviewLevel.JUNIOR);
        given(rubricScorer.score(any(), any(), any(), any(AnswerAnalysis.class)))
                .willReturn(RubricScoringResult.empty("resume-v1"));

        listener.on(event);

        then(questionScorePersister).should(times(0))
                .saveRubric(anyLong(), anyLong(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("내부 예외는 삼키고 rubric_failure 카운터 증가")
    void swallows_exception_and_increments_failure_metric() {
        AnswerAnalysisCompletedEvent event = AnswerAnalysisCompletedEvent.of(
                10L, 1L, 100L, 50L, AnswerAnalysis.empty(), InterviewLevel.JUNIOR);
        given(rubricScorer.score(any(), any(), any(), any(AnswerAnalysis.class)))
                .willThrow(new RuntimeException("AI 호출 폭발"));

        listener.on(event);

        then(aiCallMetrics).should(times(1)).incrementRubricFailure("persist_failed");
        then(questionScorePersister).should(times(0))
                .saveRubric(anyLong(), anyLong(), anyString(), any(), anyMap());
    }
}
