package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScoreEntity;
import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.vo.IntentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private RubricScoreStore rubricScoreStore;

    @Mock
    private com.rehearse.api.domain.interview.service.InterviewFinder interviewFinder;

    @Mock
    private com.rehearse.api.domain.question.repository.QuestionRepository questionRepository;

    @Mock
    private com.rehearse.api.domain.questionset.repository.QuestionSetRepository questionSetRepository;

    @Mock
    private com.rehearse.api.infra.ai.metrics.AiCallMetrics aiCallMetrics;

    @Nested
    @DisplayName("idempotent guard")
    class IdempotentGuard {

        @Test
        @DisplayName("동일 turnId로 이미 row 존재하면 채점 skip")
        void on_duplicateTurn_skipScoring() {
            TurnCompletedEvent event = createEvent(1L, 0L);
            given(rubricScoreStore.findExisting(1L, 0L))
                    .willReturn(Optional.of(mockExistingEntity()));

            listener.on(event);

            then(rubricScorer).should(never()).score(any(), any(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("정상 처리")
    class NormalProcess {

        @Test
        @DisplayName("Row 없을 때 채점 후 저장")
        void on_newTurn_scoresAndSaves() {
            TurnCompletedEvent event = createEvent(1L, 1L);
            given(rubricScoreStore.findExisting(1L, 1L)).willReturn(Optional.empty());

            com.rehearse.api.domain.interview.entity.Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            com.rehearse.api.domain.question.entity.Question question = createQuestion();
            given(questionRepository.findById(10L)).willReturn(Optional.of(question));

            com.rehearse.api.domain.questionset.entity.QuestionSet questionSet = createQuestionSet(interview);
            given(questionSetRepository.findById(20L)).willReturn(Optional.of(questionSet));

            RubricScore score = new RubricScore("concept-cs-fundamental-v1",
                    List.of("D2"), Map.of("D2", DimensionScore.of(2, "설명", "ev")), null);
            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(score);

            listener.on(event);

            then(rubricScoreStore).should().save(1L, 1L, score);
        }

        @Test
        @DisplayName("RubricScore empty면 저장 안 함")
        void on_emptyScore_noSave() {
            TurnCompletedEvent event = createEvent(1L, 2L);
            given(rubricScoreStore.findExisting(1L, 2L)).willReturn(Optional.empty());

            com.rehearse.api.domain.interview.entity.Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            com.rehearse.api.domain.question.entity.Question question = createQuestion();
            given(questionRepository.findById(10L)).willReturn(Optional.of(question));

            com.rehearse.api.domain.questionset.entity.QuestionSet questionSet = createQuestionSet(interview);
            given(questionSetRepository.findById(20L)).willReturn(Optional.of(questionSet));

            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(RubricScore.empty("resume-v1"));

            listener.on(event);

            then(rubricScoreStore).should(never()).save(anyLong(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("실패 처리")
    class FailureHandling {

        @Test
        @DisplayName("채점 예외 발생 시 예외 전파하지 않음")
        void on_scoringException_doesNotThrow() {
            TurnCompletedEvent event = createEvent(1L, 3L);
            given(rubricScoreStore.findExisting(1L, 3L)).willReturn(Optional.empty());
            given(interviewFinder.findById(anyLong()))
                    .willThrow(new RuntimeException("DB 오류"));

            assertThatNoException().isThrownBy(() -> listener.on(event));
        }
    }

    @Nested
    @DisplayName("race condition idempotent (B2/B14)")
    class RaceConditionIdempotent {

        @Test
        @DisplayName("DataIntegrityViolationException 발생 시 silent skip — 예외 전파 안 함")
        void on_dataIntegrityViolation_silentSkip() {
            TurnCompletedEvent event = createEvent(1L, 5L);
            given(rubricScoreStore.findExisting(1L, 5L)).willReturn(Optional.empty());

            com.rehearse.api.domain.interview.entity.Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            com.rehearse.api.domain.question.entity.Question question = createQuestion();
            given(questionRepository.findById(10L)).willReturn(Optional.of(question));

            com.rehearse.api.domain.questionset.entity.QuestionSet questionSet = createQuestionSet(interview);
            given(questionSetRepository.findById(20L)).willReturn(Optional.of(questionSet));

            RubricScore score = new RubricScore("resume-v1",
                    List.of("D2"), Map.of("D2", DimensionScore.of(2, "설명", "ev")), null);
            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(score);
            given(rubricScoreStore.save(1L, 5L, score))
                    .willThrow(new DataIntegrityViolationException("uk_rubric_interview_turn violation"));
            given(rubricScoreStore.findExisting(1L, 5L))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(mockExistingEntity()));

            assertThatNoException().isThrownBy(() -> listener.on(event));
        }
    }

    @Nested
    @DisplayName("Resume Track null questionId (B1)")
    class ResumeTrackNullQuestionId {

        @Test
        @DisplayName("questionId=null인 Resume Track 이벤트도 채점 수행")
        void on_resumeTrack_nullQuestionId_scoresWithStub() {
            TurnCompletedEvent event = TurnCompletedEvent.ofResumeTrack(
                    1L, 0L, 1L,
                    null, null,
                    "답변 텍스트",
                    new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 3,
                            com.rehearse.api.domain.interview.RecommendedNextAction.DEEP_DIVE),
                    IntentType.ANSWER, InterviewLevel.MID,
                    com.rehearse.api.domain.resume.domain.ResumeMode.INTERROGATION, 2, null
            );
            given(rubricScoreStore.findExisting(1L, 0L)).willReturn(Optional.empty());

            com.rehearse.api.domain.interview.entity.Interview interview = createInterview();
            given(interviewFinder.findById(1L)).willReturn(interview);

            RubricScore score = new RubricScore("resume-v1",
                    List.of("D2"), Map.of("D2", DimensionScore.of(2, "설명", "ev")), null);
            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(score);

            listener.on(event);

            then(rubricScoreStore).should().save(1L, 0L, score);
            then(questionRepository).shouldHaveNoInteractions();
            then(questionSetRepository).shouldHaveNoInteractions();
        }
    }

    private TurnCompletedEvent createEvent(Long interviewId, Long turnIndex) {
        AnswerAnalysis analysis = new AnswerAnalysis(
                turnIndex, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
        return TurnCompletedEvent.ofStandard(
                interviewId, turnIndex, 1L,
                10L, 20L,
                "답변 텍스트", analysis,
                IntentType.ANSWER, InterviewLevel.MID
        );
    }

    private RubricScoreEntity mockExistingEntity() {
        return RubricScoreEntity.builder()
                .interviewId(1L)
                .turnId(0L)
                .rubricId("concept-cs-fundamental-v1")
                .scoresJson(Map.of())
                .build();
    }

    private com.rehearse.api.domain.interview.entity.Interview createInterview() {
        return com.rehearse.api.domain.interview.entity.Interview.builder()
                .userId(1L)
                .position(com.rehearse.api.domain.interview.entity.Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(com.rehearse.api.domain.interview.entity.InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
    }

    private com.rehearse.api.domain.question.entity.Question createQuestion() {
        return com.rehearse.api.domain.question.entity.Question.builder()
                .questionType(com.rehearse.api.domain.question.entity.QuestionType.MAIN)
                .questionText("테스트 질문")
                .feedbackPerspective(com.rehearse.api.domain.feedback.entity.FeedbackPerspective.TECHNICAL)
                .orderIndex(0)
                .build();
    }

    private com.rehearse.api.domain.questionset.entity.QuestionSet createQuestionSet(
            com.rehearse.api.domain.interview.entity.Interview interview) {
        return com.rehearse.api.domain.questionset.entity.QuestionSet.builder()
                .interview(interview)
                .category(com.rehearse.api.domain.questionset.entity.QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
    }
}
