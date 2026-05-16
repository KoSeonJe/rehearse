package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.event.FollowUpQuestionCreatedEvent;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
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
    public void on(FollowUpQuestionCreatedEvent event) {
        Long interviewId = event.interviewId();

        try {
            Interview interview = interviewFinder.findById(interviewId);
            Question question = resolveQuestion(event);
            QuestionSet questionSet = resolveQuestionSet(event, interview);

            RubricScoringResult score = rubricScorer.score(
                    question, questionSet, interview,
                    event.userAnswer(), event.analysis()
            );

            if (score.isEmpty()) {
                log.debug("[정상 skip] RubricScore empty. interviewId={}, questionId={}",
                        interviewId, event.questionId());
                return;
            }

            questionScorePersister.saveRubric(
                    question.getId(), interviewId,
                    score.rubricId(), score.levelFlag(),
                    score.dimensionScores()
            );

        } catch (Exception e) {
            log.warn("[결함 skip] RubricScoring listener 예외. interviewId={}, reason={}",
                    interviewId, e.getMessage(), e);
            aiCallMetrics.incrementRubricFailure("persist_failed");
        }
    }

    private Question resolveQuestion(FollowUpQuestionCreatedEvent event) {
        if (event.questionId() == null) {
            throw new IllegalStateException(
                    "Question 식별 불가 — questionId null. interviewId=" + event.interviewId());
        }
        return questionRepository.findById(event.questionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Question 미존재: questionId=" + event.questionId()
                                + ", interviewId=" + event.interviewId()));
    }

    private QuestionSet resolveQuestionSet(FollowUpQuestionCreatedEvent event, Interview interview) {
        if (event.questionSetId() == null) {
            throw new IllegalStateException(
                    "QuestionSet 식별 불가 — questionSetId null. interviewId=" + event.interviewId()
                            + ", questionId=" + event.questionId());
        }
        return questionSetRepository.findById(event.questionSetId())
                .orElseThrow(() -> new IllegalStateException(
                        "QuestionSet 미존재: questionSetId=" + event.questionSetId()
                                + ", interviewId=" + event.interviewId()
                                + ", questionId=" + event.questionId()));
    }
}
