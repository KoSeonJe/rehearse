package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.config.RubricScoringExecutorConfig;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScoringEventListener {

    private final RubricScorer rubricScorer;
    private final RubricScoreStore rubricScoreStore;
    private final InterviewFinder interviewFinder;
    private final QuestionRepository questionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final AiCallMetrics aiCallMetrics;

    @Async(RubricScoringExecutorConfig.RUBRIC_SCORING_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TurnCompletedEvent event) {
        Long interviewId = event.interviewId();
        Long turnIndex = event.turnIndex();

        if (rubricScoreStore.findExisting(interviewId, turnIndex).isPresent()) {
            log.debug("RubricScore 이미 존재 — skip (idempotent): interviewId={}, turnId={}", interviewId, turnIndex);
            return;
        }

        try {
            Interview interview = interviewFinder.findById(interviewId);

            Question question = resolveQuestion(event);
            QuestionSet questionSet = resolveQuestionSet(event, interview);

            RubricScore score = rubricScorer.score(
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

            saveIdempotent(interviewId, turnIndex, score);

        } catch (Exception e) {
            log.warn("RubricScoring 실패 — 턴 진행 차단하지 않음: interviewId={}, turnId={}, reason={}",
                    interviewId, turnIndex, e.getMessage());
            aiCallMetrics.incrementRubricFailure("persist_failed");
        }
    }

    private Question resolveQuestion(TurnCompletedEvent event) {
        if (event.questionId() == null) {
            // Resume Track: LLM이 동적 생성한 질문 — DB 조회 불가, stub 사용
            return Question.stubForResumeTrack();
        }
        return questionRepository.findById(event.questionId())
                .orElseThrow(() -> new IllegalStateException("Question not found: " + event.questionId()));
    }

    private QuestionSet resolveQuestionSet(TurnCompletedEvent event, Interview interview) {
        if (event.questionSetId() == null) {
            // Resume Track: questionSet 없음 — rubric resolution은 resumeMode 우선이므로 stub 사용
            return QuestionSet.stubForResumeTrack(interview);
        }
        return questionSetRepository.findById(event.questionSetId())
                .orElseThrow(() -> new IllegalStateException("QuestionSet not found: " + event.questionSetId()));
    }

    private void saveIdempotent(Long interviewId, Long turnIndex, RubricScore score) {
        try {
            rubricScoreStore.save(interviewId, turnIndex, score);
            log.info("RubricScore 저장 완료: interviewId={}, turnId={}, rubricId={}, scored={}",
                    interviewId, turnIndex, score.rubricId(), score.scoredDimensions());
        } catch (DataIntegrityViolationException e) {
            // race condition: 동시 요청으로 UNIQUE 제약 위반 → 이미 존재하므로 silent skip
            log.debug("RubricScore race condition — 이미 존재함, silent skip: interviewId={}, turnId={}", interviewId, turnIndex);
            rubricScoreStore.findExisting(interviewId, turnIndex)
                    .ifPresent(existing -> log.debug("기존 row 확인: id={}", existing.getId()));
        }
    }
}
