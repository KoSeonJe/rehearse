package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.exception.QuestionErrorCode;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StandardFollowUpPolicy implements InterviewTurnPolicy {

    private static final int RESUME_MAX_ROUNDS_PER_MAIN = 2;

    private final int maxRounds;

    public StandardFollowUpPolicy(
            @Value("${rehearse.interview.policy.standard.max-follow-up-rounds:2}") int maxRounds) {
        this.maxRounds = maxRounds;
    }

    @Override
    public InterviewTrack getTrack() {
        return InterviewTrack.CS;
    }

    @Override
    public int getMaxFollowUpRounds() {
        return maxRounds;
    }

    @Override
    public void assertCanContinue(Interview interview, QuestionSet questionSet) {
        assertCanContinue(interview, questionSet, null);
    }

    public void assertCanContinue(Interview interview, QuestionSet questionSet, Long currentMainQuestionId) {
        if (isFollowUpExhausted(questionSet, currentMainQuestionId)) {
            throw new BusinessException(QuestionErrorCode.MAX_FOLLOWUP_EXCEEDED);
        }
    }

    public boolean isFollowUpExhausted(QuestionSet questionSet, Long currentMainQuestionId) {
        long followUpCount = countFollowUpsForCurrentMain(questionSet, currentMainQuestionId);
        int cap = resolveCap(questionSet, currentMainQuestionId);
        return followUpCount >= cap;
    }

    public boolean shouldSkipFollowUp(QuestionSet questionSet, Long currentMainQuestionId) {
        QuestionType currentType = resolveCurrentMainType(questionSet, currentMainQuestionId);
        return currentType == QuestionType.RESUME_OPENER;
    }

    private int resolveCap(QuestionSet questionSet, Long currentMainQuestionId) {
        QuestionType currentType = resolveCurrentMainType(questionSet, currentMainQuestionId);
        if (currentType != null && currentType.isResume()) {
            return RESUME_MAX_ROUNDS_PER_MAIN;
        }
        return maxRounds;
    }

    private QuestionType resolveCurrentMainType(QuestionSet questionSet, Long currentMainQuestionId) {
        if (currentMainQuestionId != null) {
            return questionSet.getQuestions().stream()
                    .filter(q -> currentMainQuestionId.equals(q.getId()))
                    .map(Question::getQuestionType)
                    .findFirst()
                    .orElse(null);
        }
        return questionSet.getQuestions().stream()
                .filter(q -> !q.getQuestionType().isFollowUp())
                .map(Question::getQuestionType)
                .findFirst()
                .orElse(null);
    }

    private long countFollowUpsForCurrentMain(QuestionSet questionSet, Long currentMainQuestionId) {
        List<Question> questions = questionSet.getQuestions();
        if (currentMainQuestionId == null) {
            return questions.stream().filter(q -> q.getQuestionType().isFollowUp()).count();
        }

        int currentMainOrder = -1;
        int nextMainOrder = Integer.MAX_VALUE;
        for (Question q : questions) {
            if (currentMainQuestionId.equals(q.getId())) {
                currentMainOrder = q.getOrderIndex();
            }
        }
        if (currentMainOrder < 0) {
            return 0;
        }
        for (Question q : questions) {
            if (!q.getQuestionType().isFollowUp()
                    && q.getOrderIndex() > currentMainOrder
                    && q.getOrderIndex() < nextMainOrder) {
                nextMainOrder = q.getOrderIndex();
            }
        }
        int finalCurrentMainOrder = currentMainOrder;
        int finalNextMainOrder = nextMainOrder;
        return questions.stream()
                .filter(q -> q.getQuestionType().isFollowUp())
                .filter(q -> q.getOrderIndex() > finalCurrentMainOrder
                        && q.getOrderIndex() < finalNextMainOrder)
                .count();
    }
}
