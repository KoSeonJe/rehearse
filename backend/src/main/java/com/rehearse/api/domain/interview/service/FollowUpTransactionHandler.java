package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionType;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FollowUpTransactionHandler {

    private static final int MAX_FOLLOWUP_ROUNDS = 2;

    private final InterviewFinder interviewFinder;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public FollowUpContext loadFollowUpContext(Long interviewId, Long questionSetId) {
        Interview interview = interviewFinder.findById(interviewId);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
        }

        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        validateFollowUpRoundLimit(questionSet);
        int nextOrderIndex = questionSet.getQuestions().size();

        return new FollowUpContext(
                interview.getPosition(),
                interview.getEffectiveTechStack(),
                interview.getLevel(),
                questionSetId,
                nextOrderIndex
        );
    }

    @Transactional
    public Question saveFollowUpResult(Long questionSetId, GeneratedFollowUp followUp, int orderIndex) {
        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        Question followUpQuestion = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText(followUp.getQuestion())
                .modelAnswer(followUp.getModelAnswer())
                .orderIndex(orderIndex)
                .build();

        questionSet.addQuestion(followUpQuestion);
        return questionRepository.save(followUpQuestion);
    }

    private void validateFollowUpRoundLimit(QuestionSet questionSet) {
        long followUpCount = questionSet.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
                .count();

        if (followUpCount >= MAX_FOLLOWUP_ROUNDS) {
            throw new BusinessException(QuestionSetErrorCode.MAX_FOLLOWUP_EXCEEDED);
        }
    }
}
