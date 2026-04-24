package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.policy.InterviewTurnPolicyResolver;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FollowUpTransactionHandler {

    private final InterviewFinder interviewFinder;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final InterviewTurnPolicyResolver turnPolicyResolver;

    @Transactional(readOnly = true)
    public FollowUpContext loadFollowUpContext(Long interviewId, Long userId, Long questionSetId) {
        Interview interview = interviewFinder.findByIdAndValidateOwner(interviewId, userId);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
        }

        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        turnPolicyResolver.resolve(interview).assertCanContinue(interview, questionSet);
        int nextOrderIndex = questionSet.getQuestions().size();
        ReferenceType mainReferenceType = resolveMainReferenceType(questionSet);

        return new FollowUpContext(
                interview.getPosition(),
                interview.getEffectiveTechStack(),
                interview.getLevel(),
                questionSetId,
                nextOrderIndex,
                mainReferenceType
        );
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
    public Question saveFollowUpResult(Long questionSetId, GeneratedFollowUp followUp, int orderIndex) {
        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        Question followUpQuestion = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText(followUp.getQuestion())
                .ttsText(followUp.getTtsQuestion())
                .modelAnswer(followUp.getModelAnswer())
                .orderIndex(orderIndex)
                .build();

        questionSet.addQuestion(followUpQuestion);
        return questionRepository.save(followUpQuestion);
    }

}
