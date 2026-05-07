package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpTransactionHandler {

    private final InterviewFinder interviewFinder;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final InterviewTurnPolicyResolver turnPolicyResolver;
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

        var policy = turnPolicyResolver.resolve(interview);
        policy.assertCanContinue(interview, questionSet);
        int nextOrderIndex = questionSet.getQuestions().size();
        ReferenceType mainReferenceType = resolveMainReferenceType(questionSet);
        Long currentMainQuestionId = resolveCurrentMainQuestionId(questionSet);

        return new FollowUpContext(
                interview.getPosition(),
                interview.getEffectiveTechStack(),
                interview.getLevel(),
                questionSetId,
                currentMainQuestionId,
                nextOrderIndex,
                mainReferenceType,
                policy.getMaxFollowUpRounds()
        );
    }

    private Long resolveCurrentMainQuestionId(QuestionSet questionSet) {
        return questionSet.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.MAIN)
                .findFirst()
                .map(Question::getId)
                .orElse(null);
    }

    /**
     * 메인 질문의 referenceType을 추출해 후속질문 프롬프트 모드 분기에 사용한다.
     * MODEL_ANSWER → CS 개념 설명형 메인 질문 → CONCEPT 모드
     * GUIDE        → 이력서·경험 기반 메인 질문 → EXPERIENCE 모드
     * 메인 질문이 없거나 referenceType이 null인 엣지 케이스는 안전한 기본값(MODEL_ANSWER)으로 폴백한다.
     * 경험 전제 프레이밍이 안 나가는 쪽이 어색함보다 덜 위험하기 때문.
     */
    private ReferenceType resolveMainReferenceType(QuestionSet questionSet) {
        return questionSet.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.MAIN)
                .findFirst()
                .map(Question::getReferenceType)
                .filter(rt -> rt != null)
                .orElse(ReferenceType.MODEL_ANSWER);
    }

    @Transactional
    public FollowUpSaveResult saveFollowUpResult(Long questionSetId, GeneratedFollowUp followUp) {
        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        int orderIndex = questionSet.getQuestions().size();

        Question followUpQuestion = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText(followUp.getQuestion())
                .ttsText(followUp.getTtsQuestion())
                .modelAnswer(followUp.getModelAnswer())
                .orderIndex(orderIndex)
                .build();

        questionSet.addQuestion(followUpQuestion);
        try {
            Question saved = questionRepository.saveAndFlush(followUpQuestion);
            int newFollowUpCount = (int) questionSet.getQuestions().stream()
                    .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
                    .count();
            return new FollowUpSaveResult(saved, newFollowUpCount);
        } catch (DataIntegrityViolationException e) {
            log.warn("follow-up 중복 삽입 차단 (unique constraint): questionSetId={}, orderIndex={}",
                    questionSetId, orderIndex);
            throw new BusinessException(InterviewErrorCode.FOLLOWUP_DUPLICATE);
        }
    }

    @Transactional
    public FollowUpSaveResult saveFollowUpResultAndPublishEvent(
            Long interviewId, FollowUpContext context, GeneratedFollowUp followUp, TurnAnalysisResult turn
    ) {
        FollowUpSaveResult saveResult = saveFollowUpResult(context.questionSetId(), followUp);
        publishTurnCompletedEvent(
                interviewId, context, turn,
                saveResult.question().getId(), saveResult.question().getOrderIndex());
        return saveResult;
    }

    @Transactional
    public void publishTurnCompletedEvent(
            Long interviewId, FollowUpContext context, TurnAnalysisResult turn, Long questionId, int turnIndex
    ) {
        try {
            Interview interview = interviewFinder.findById(interviewId);
            TurnCompletedEvent event = TurnCompletedEvent.ofStandard(
                    interviewId, (long) turnIndex, interview.getUserId(),
                    questionId, context.questionSetId(),
                    turn.answerText(), turn.answerAnalysis(),
                    context.level()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("TurnCompletedEvent 발행 실패. interviewId={}, reason={}",
                    interviewId, e.getMessage());
        }
    }
}
