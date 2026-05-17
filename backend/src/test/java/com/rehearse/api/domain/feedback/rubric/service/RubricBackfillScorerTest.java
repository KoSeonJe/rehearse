package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RubricBackfillScorer — 보강 채점 단위 테스트")
class RubricBackfillScorerTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionSetRepository questionSetRepository;
    @Mock private TimestampFeedbackRepository timestampFeedbackRepository;
    @Mock private InterviewFinder interviewFinder;
    @Mock private RubricScorer rubricScorer;
    @Mock private QuestionScorePersister questionScorePersister;
    @Mock private AiCallMetrics aiCallMetrics;

    @InjectMocks
    private RubricBackfillScorer backfillScorer;

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
    }

    @Test
    @DisplayName("대상 없음 → RubricScorer 호출 0회")
    void no_targets_skip() {
        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findAnsweredButUnscoredByInterviewId(10L)).willReturn(List.of());

        backfillScorer.backfill(10L);

        then(rubricScorer).should(times(0)).score(any(), any(), any(), anyString(), any());
        then(questionScorePersister).should(times(0)).saveRubric(anyLong(), anyLong(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("transcript 가 있고 채점 결과 비어있지 않으면 saveRubric 호출")
    void persists_score_when_scoring_result_not_empty() {
        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findAnsweredButUnscoredByInterviewId(10L)).willReturn(List.of(question));
        given(questionSetRepository.findById(50L)).willReturn(Optional.of(questionSet));
        TimestampFeedback timestampFeedback = TestFixtures.createTimestampFeedback(question);
        ReflectionTestUtils.setField(timestampFeedback, "transcript", "실제 답변");
        given(timestampFeedbackRepository.findByQuestionId(100L)).willReturn(List.of(timestampFeedback));

        Map<String, DimensionScore> dims = Map.of("clarity", new DimensionScore(4, "ok", "quote"));
        RubricScoringResult result = new RubricScoringResult("resume-v1", List.of("clarity"), dims, "L2");
        given(rubricScorer.score(eq(question), eq(questionSet), eq(interview), eq("실제 답변"), any(AnswerAnalysis.class)))
                .willReturn(result);

        backfillScorer.backfill(10L);

        then(questionScorePersister).should(times(1))
                .saveRubric(100L, 10L, "resume-v1", "L2", dims);
    }

    @Test
    @DisplayName("transcript 비어있으면 skip — RubricScorer 호출 0회")
    void skips_when_transcript_blank() {
        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findAnsweredButUnscoredByInterviewId(10L)).willReturn(List.of(question));
        given(questionSetRepository.findById(50L)).willReturn(Optional.of(questionSet));
        given(timestampFeedbackRepository.findByQuestionId(100L)).willReturn(List.of());

        backfillScorer.backfill(10L);

        then(rubricScorer).should(times(0)).score(any(), any(), any(), anyString(), any());
        then(questionScorePersister).should(times(0)).saveRubric(anyLong(), anyLong(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("채점 결과 empty 면 saveRubric 호출 안 함")
    void skips_persist_when_scoring_empty() {
        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findAnsweredButUnscoredByInterviewId(10L)).willReturn(List.of(question));
        given(questionSetRepository.findById(50L)).willReturn(Optional.of(questionSet));
        TimestampFeedback timestampFeedback = TestFixtures.createTimestampFeedback(question);
        ReflectionTestUtils.setField(timestampFeedback, "transcript", "답변");
        given(timestampFeedbackRepository.findByQuestionId(100L)).willReturn(List.of(timestampFeedback));
        given(rubricScorer.score(any(), any(), any(), anyString(), any()))
                .willReturn(RubricScoringResult.empty("resume-v1"));

        backfillScorer.backfill(10L);

        then(questionScorePersister).should(times(0)).saveRubric(anyLong(), anyLong(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("transcript 다중 segment 는 공백으로 결합되어 RubricScorer 에 전달된다")
    void joins_multiple_transcript_segments() {
        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findAnsweredButUnscoredByInterviewId(10L)).willReturn(List.of(question));
        given(questionSetRepository.findById(50L)).willReturn(Optional.of(questionSet));

        TimestampFeedback seg1 = TestFixtures.createTimestampFeedback(question);
        ReflectionTestUtils.setField(seg1, "transcript", "첫 번째 segment");
        TimestampFeedback seg2 = TestFixtures.createTimestampFeedback(question);
        ReflectionTestUtils.setField(seg2, "transcript", "두 번째 segment");
        given(timestampFeedbackRepository.findByQuestionId(100L)).willReturn(List.of(seg1, seg2));

        Map<String, DimensionScore> dims = Map.of("clarity", new DimensionScore(4, "ok", "quote"));
        given(rubricScorer.score(eq(question), eq(questionSet), eq(interview),
                eq("첫 번째 segment 두 번째 segment"), any(AnswerAnalysis.class)))
                .willReturn(new RubricScoringResult("resume-v1", List.of("clarity"), dims, "L2"));

        backfillScorer.backfill(10L);

        then(questionScorePersister).should(times(1))
                .saveRubric(100L, 10L, "resume-v1", "L2", dims);
    }

    @Test
    @DisplayName("RubricScorer 예외 → 메트릭 증가 후 다음 질문 진행")
    void increments_metric_and_continues_on_scorer_exception() {
        Question q1 = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
        q1.assignQuestionSet(questionSet);
        ReflectionTestUtils.setField(q1, "id", 101L);
        Question q2 = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
        q2.assignQuestionSet(questionSet);
        ReflectionTestUtils.setField(q2, "id", 102L);

        given(interviewFinder.findById(10L)).willReturn(interview);
        given(questionRepository.findAnsweredButUnscoredByInterviewId(10L)).willReturn(List.of(q1, q2));
        given(questionSetRepository.findById(50L)).willReturn(Optional.of(questionSet));

        TimestampFeedback tf1 = TestFixtures.createTimestampFeedback(q1);
        ReflectionTestUtils.setField(tf1, "transcript", "답변1");
        TimestampFeedback tf2 = TestFixtures.createTimestampFeedback(q2);
        ReflectionTestUtils.setField(tf2, "transcript", "답변2");
        given(timestampFeedbackRepository.findByQuestionId(101L)).willReturn(List.of(tf1));
        given(timestampFeedbackRepository.findByQuestionId(102L)).willReturn(List.of(tf2));

        willThrow(new RuntimeException("AI 호출 실패"))
                .given(rubricScorer).score(eq(q1), any(), any(), anyString(), any());

        Map<String, DimensionScore> dims = Map.of("clarity", new DimensionScore(3, "ok", "quote"));
        given(rubricScorer.score(eq(q2), any(), any(), anyString(), any()))
                .willReturn(new RubricScoringResult("resume-v1", List.of("clarity"), dims, "L2"));

        backfillScorer.backfill(10L);

        then(aiCallMetrics).should(times(1)).incrementRubricFailure("backfill_failed");
        then(questionScorePersister).should(times(1))
                .saveRubric(eq(102L), eq(10L), eq("resume-v1"), eq("L2"), eq(dims));
    }
}
