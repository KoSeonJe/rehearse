package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.event.AnswerAnalysisCompletedEvent;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpTransactionHandler {

    private final InterviewFinder interviewFinder;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final StandardFollowUpPolicy standardFollowUpPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public FollowUpContext loadFollowUpContext(Long interviewId, Long userId, Long questionSetId) {
        Interview interview = interviewFinder.findById(interviewId);
        interview.validateOwner(userId);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
        }

        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        Optional<Question> currentMainQuestion = findCurrentMainQuestion(questionSet);
        Long currentMainQuestionId = currentMainQuestion.map(Question::getId).orElse(null);
        QuestionType currentMainQuestionType = currentMainQuestion.map(Question::getQuestionType).orElse(null);
        standardFollowUpPolicy.assertCanContinue(interview, questionSet, currentMainQuestionId);
        int nextOrderIndex = questionSet.getQuestions().size();
        ReferenceType mainReferenceType = resolveMainReferenceType(questionSet);

        return new FollowUpContext(
                interview.getPosition(),
                interview.getEffectiveTechStack(),
                interview.getLevel(),
                questionSetId,
                currentMainQuestionId,
                currentMainQuestionType,
                nextOrderIndex,
                mainReferenceType,
                standardFollowUpPolicy.getMaxFollowUpRounds()
        );
    }

    private ReferenceType resolveMainReferenceType(QuestionSet questionSet) {
        InterviewType category = questionSet.getCategory();
        return findCurrentMainQuestion(questionSet)
                .map(q -> q.getQuestionType().referenceType())
                .orElseGet(() -> category == InterviewType.BEHAVIORAL
                        ? ReferenceType.GUIDE
                        : ReferenceType.MODEL_ANSWER);
    }

    private Optional<Question> findCurrentMainQuestion(QuestionSet questionSet) {
        return questionSet.getQuestions().stream()
                .filter(q -> !q.getQuestionType().isFollowUp())
                .reduce((first, second) -> second);
    }

    @Transactional
    public FollowUpSaveResult saveFollowUpResult(Long questionSetId, GeneratedFollowUp followUp) {
        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        int orderIndex = questionSet.getQuestions().size();
        QuestionType followUpType = resolveFollowUpType(questionSet);

        Question followUpQuestion = Question.builder()
                .questionType(followUpType)
                .questionText(followUp.question())
                .ttsText(followUp.ttsQuestion())
                .bestAnswer(followUp.bestAnswer())
                .orderIndex(orderIndex)
                .build();

        questionSet.addQuestion(followUpQuestion);
        try {
            Question saved = questionRepository.saveAndFlush(followUpQuestion);
            int newFollowUpCount = (int) questionSet.getQuestions().stream()
                    .filter(q -> q.getQuestionType().isFollowUp())
                    .count();
            return new FollowUpSaveResult(saved, newFollowUpCount);
        } catch (DataIntegrityViolationException e) {
            log.warn("follow-up 중복 삽입 차단 (unique constraint): questionSetId={}, orderIndex={}",
                    questionSetId, orderIndex);
            throw new BusinessException(InterviewErrorCode.FOLLOWUP_DUPLICATE);
        }
    }

    private QuestionType resolveFollowUpType(QuestionSet questionSet) {
        QuestionType mainType = findCurrentMainQuestion(questionSet)
                .map(Question::getQuestionType)
                .orElse(null);
        if (mainType == QuestionType.TECH_MAIN) {
            return QuestionType.TECH_FOLLOWUP;
        }
        if (mainType == QuestionType.BEHAVIORAL_MAIN) {
            return QuestionType.BEHAVIORAL_FOLLOWUP;
        }
        if (mainType == QuestionType.RESUME_MAIN) {
            return QuestionType.RESUME_FOLLOWUP;
        }
        return questionSet.getCategory() == InterviewType.BEHAVIORAL
                ? QuestionType.BEHAVIORAL_FOLLOWUP
                : QuestionType.TECH_FOLLOWUP;
    }

    @Transactional
    public void publishAnswerAnalysisCompletedEvent(
            Long interviewId, FollowUpContext context,
            AnswerAnalysis analysis, String userAnswer, Long questionId
    ) {
        try {
            Interview interview = interviewFinder.findById(interviewId);
            AnswerAnalysisCompletedEvent event = AnswerAnalysisCompletedEvent.of(
                    interviewId, interview.getUserId(),
                    questionId, context.questionSetId(),
                    userAnswer, analysis,
                    context.level()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("AnswerAnalysisCompletedEvent 발행 실패. interviewId={}, reason={}",
                    interviewId, e.getMessage());
        }
    }
}
