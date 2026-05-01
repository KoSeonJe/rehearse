package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.config.RubricScoringExecutorConfig;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScoringEventListener {

    private final RubricScorer rubricScorer;
    private final QuestionScorePersister questionScorePersister;
    private final InterviewFinder interviewFinder;
    private final QuestionRepository questionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final AiCallMetrics aiCallMetrics;

    @Async(RubricScoringExecutorConfig.RUBRIC_SCORING_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TurnCompletedEvent event) {
        Long interviewId = event.interviewId();
        Long turnIndex = event.turnIndex();

        try {
            Interview interview = interviewFinder.findById(interviewId);

            Question question = resolveQuestion(event);
            if (question == null) {
                return;
            }

            if (question.getId() == null) {
                log.warn("questionId null — QuestionScore 저장 불가 (stub 사용 중): interviewId={}, turnIndex={}", interviewId, turnIndex);
                return;
            }

            QuestionSet questionSet = resolveQuestionSet(event, interview);
            if (questionSet == null) {
                return;
            }

            RubricScoringResult score = rubricScorer.score(
                    question, questionSet, interview,
                    event.userAnswer(), event.analysis(),
                    event.intent(), event.resumeMode(),
                    event.currentChainLevel(), event.resumeSkeleton()
            );

            if (score.isEmpty()) {
                log.debug("RubricScore empty (CLARIFY 등) — row 적재 안 함: interviewId={}, turnId={}",
                        interviewId, turnIndex);
                return;
            }

            String feedbackPerspective = question.getFeedbackPerspective() != null
                    ? question.getFeedbackPerspective().name() : null;

            questionScorePersister.saveRubric(
                    question.getId(), interviewId,
                    score.rubricId(), score.levelFlag(), feedbackPerspective,
                    score.dimensionScores()
            );

        } catch (Exception e) {
            log.warn("RubricScoring 실패 — 턴 진행 차단하지 않음: interviewId={}, turnId={}, reason={}",
                    interviewId, turnIndex, e.getMessage());
            aiCallMetrics.incrementRubricFailure("persist_failed");
        }
    }

    private Question resolveQuestion(TurnCompletedEvent event) {
        if (event.questionId() == null) {
            log.warn("questionId가 null — RubricScoring 스킵: interviewId={}, turnIndex={}",
                    event.interviewId(), event.turnIndex());
            return null;
        }
        return questionRepository.findById(event.questionId())
                .orElseThrow(() -> new IllegalStateException("Question not found: " + event.questionId()));
    }

    private QuestionSet resolveQuestionSet(TurnCompletedEvent event, Interview interview) {
        if (event.questionSetId() == null) {
            log.warn("questionSetId가 null — RubricScoring 스킵: interviewId={}, turnIndex={}",
                    event.interviewId(), event.turnIndex());
            return null;
        }
        return questionSetRepository.findById(event.questionSetId())
                .orElseThrow(() -> new IllegalStateException("QuestionSet not found: " + event.questionSetId()));
    }
}
