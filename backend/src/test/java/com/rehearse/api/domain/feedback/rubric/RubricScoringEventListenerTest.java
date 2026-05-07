package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.feedback.rubric.service.RubricScorer;
import com.rehearse.api.domain.feedback.rubric.service.RubricScoringEventListener;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RubricScoringEventListener")
class RubricScoringEventListenerTest {

    @InjectMocks
    private RubricScoringEventListener listener;

    @Mock
    private RubricScorer rubricScorer;

    @Mock
    private QuestionScorePersister questionScorePersister;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private AiCallMetrics aiCallMetrics;

    @Nested
    @DisplayName("정상 처리")
    class NormalProcess {

        @Test
        @DisplayName("Row 없을 때 채점 후 QuestionScorePersister.saveRubric() 호출")
        void on_newTurn_scoresAndSaves() {
            TurnCompletedEvent event = createEvent(1L, 1L);

            Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            Question question = createQuestionWithId(10L);
            given(questionRepository.findById(10L)).willReturn(Optional.of(question));

            QuestionSet questionSet = createQuestionSet(interview);
            given(questionSetRepository.findById(20L)).willReturn(Optional.of(questionSet));

            RubricScoringResult score = new RubricScoringResult("concept-cs-fundamental-v1",
                    List.of("technical_depth"), Map.of("technical_depth", DimensionScore.of(2, "설명", "ev")), null);
            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(score);

            listener.on(event);

            then(questionScorePersister).should().saveRubric(
                    eq(10L), eq(1L),
                    eq("concept-cs-fundamental-v1"), eq(null), eq("TECHNICAL"),
                    any()
            );
        }

        @Test
        @DisplayName("RubricScore empty면 saveRubric() 호출 안 함")
        void on_emptyScore_noSave() {
            TurnCompletedEvent event = createEvent(1L, 2L);

            Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            Question question = createQuestionWithId(10L);
            given(questionRepository.findById(10L)).willReturn(Optional.of(question));

            QuestionSet questionSet = createQuestionSet(interview);
            given(questionSetRepository.findById(20L)).willReturn(Optional.of(questionSet));

            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(RubricScoringResult.empty("resume-v1"));

            listener.on(event);

            then(questionScorePersister).should(never()).saveRubric(anyLong(), anyLong(), anyString(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("실패 처리")
    class FailureHandling {

        @Test
        @DisplayName("채점 예외 발생 시 예외 전파하지 않고 결함 skip metric 증가")
        void on_scoringException_doesNotThrow() {
            TurnCompletedEvent event = createEvent(1L, 3L);
            given(interviewFinder.findById(anyLong()))
                    .willThrow(new RuntimeException("DB 오류"));

            assertThatNoException().isThrownBy(() -> listener.on(event));
            then(aiCallMetrics).should().incrementRubricFailure("persist_failed");
        }
    }

    @Nested
    @DisplayName("Resume Track null questionId (B1)")
    class ResumeTrackNullQuestionId {

        @Test
        @DisplayName("questionId=null인 Resume Track 이벤트는 결함 skip — score/saveRubric 호출 안 하고 metric 증가")
        void on_resumeTrack_nullQuestionId_skipsScoring() {
            TurnCompletedEvent event = TurnCompletedEvent.ofResumeTrack(
                    1L, 0L, 1L,
                    null, null,
                    "답변 텍스트",
                    new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 3,
                            RecommendedNextAction.DEEP_DIVE),
                    InterviewLevel.MID,
                    ResumeMode.INTERROGATION, 2, null
            );

            Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            listener.on(event);

            then(rubricScorer).should(never()).score(any(), any(), any(), any(), any(), any(), any(), any());
            then(questionScorePersister).should(never()).saveRubric(anyLong(), anyLong(), anyString(), any(), any(), any());
            then(aiCallMetrics).should().incrementRubricFailure("persist_failed");
        }

        @Test
        @DisplayName("questionSetId=null이면 결함 skip — score/saveRubric 호출 안 하고 metric 증가")
        void on_nullQuestionSetId_skipsScoring() {
            TurnCompletedEvent event = TurnCompletedEvent.ofStandard(
                    1L, 5L, 1L,
                    10L, null,
                    "답변 텍스트",
                    new AnswerAnalysis(5L, List.of(), List.of(), List.of(), 3,
                            RecommendedNextAction.DEEP_DIVE),
                    InterviewLevel.MID
            );

            Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(questionRepository.findById(10L)).willReturn(Optional.of(createQuestionWithId(10L)));

            listener.on(event);

            then(rubricScorer).should(never()).score(any(), any(), any(), any(), any(), any(), any(), any());
            then(questionScorePersister).should(never()).saveRubric(anyLong(), anyLong(), anyString(), any(), any(), any());
            then(aiCallMetrics).should().incrementRubricFailure("persist_failed");
        }
    }

    private TurnCompletedEvent createEvent(Long interviewId, Long turnIndex) {
        AnswerAnalysis analysis = new AnswerAnalysis(
                turnIndex, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
        return TurnCompletedEvent.ofStandard(
                interviewId, turnIndex, 1L,
                10L, 20L,
                "답변 텍스트", analysis,
                InterviewLevel.MID
        );
    }

    private Question createQuestionWithId(Long id) {
        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("테스트 질문")
                .feedbackPerspective(FeedbackPerspective.TECHNICAL)
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Interview createInterview() {
        return Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
    }

    private QuestionSet createQuestionSet(Interview interview) {
        return QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
    }
}
