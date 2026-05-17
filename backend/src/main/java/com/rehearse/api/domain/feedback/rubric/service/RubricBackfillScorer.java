package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricBackfillScorer {

    private final QuestionRepository questionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final TimestampFeedbackRepository timestampFeedbackRepository;
    private final InterviewFinder interviewFinder;
    private final RubricScorer rubricScorer;
    private final QuestionScorePersister questionScorePersister;
    private final AiCallMetrics aiCallMetrics;

    public void backfill(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        List<Question> unscored = questionRepository.findAnsweredButUnscoredByInterviewId(interviewId);

        if (unscored.isEmpty()) {
            log.debug("보강 채점 대상 없음: interviewId={}", interviewId);
            return;
        }

        log.info("보강 채점 시작: interviewId={}, targets={}", interviewId, unscored.size());

        for (Question question : unscored) {
            backfillSingle(interview, question);
        }
    }

    private void backfillSingle(Interview interview, Question question) {
        Long interviewId = interview.getId();
        Long questionId = question.getId();
        try {
            QuestionSet questionSet = questionSetRepository.findById(question.getQuestionSet().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "QuestionSet 미존재: questionId=" + questionId + ", interviewId=" + interviewId));

            String userAnswer = resolveUserAnswer(questionId);
            if (userAnswer == null || userAnswer.isBlank()) {
                log.debug("보강 채점 skip — 답변 transcript 없음: questionId={}", questionId);
                return;
            }

            RubricScoringResult score = rubricScorer.score(
                    question, questionSet, interview,
                    userAnswer, AnswerAnalysis.empty()
            );

            if (score.isEmpty()) {
                log.debug("보강 채점 skip — RubricScore empty: questionId={}, rubricId={}",
                        questionId, score.rubricId());
                return;
            }

            questionScorePersister.saveRubric(
                    questionId, interviewId,
                    score.rubricId(), score.levelFlag(), score.dimensionScores()
            );
        } catch (Exception e) {
            log.warn("보강 채점 실패 skip — questionId={}, reason={}", questionId, e.getMessage(), e);
            aiCallMetrics.incrementRubricFailure("backfill_failed");
        }
    }

    private String resolveUserAnswer(Long questionId) {
        return timestampFeedbackRepository.findByQuestionId(questionId).stream()
                .map(TimestampFeedback::getTranscript)
                .filter(transcript -> transcript != null && !transcript.isBlank())
                .collect(Collectors.joining(" "));
    }
}
