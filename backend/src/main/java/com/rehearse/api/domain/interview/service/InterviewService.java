package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.dto.UpdateStatusRequest;
import com.rehearse.api.domain.interview.dto.UpdateStatusResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.QuestionGenerationStatus;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.domain.questionset.service.QuestionSetService;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
import com.rehearse.api.global.config.InterviewProperties;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewFinder interviewFinder;
    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetService questionSetService;
    private final ApplicationEventPublisher eventPublisher;
    private final ResumeSkeletonRepository resumeSkeletonRepository;
    private final InterviewRetryRecorder interviewRetryRecorder;
    private final InterviewProperties interviewProperties;

    @Transactional
    public UpdateStatusResponse updateStatus(Long id, Long userId, UpdateStatusRequest request) {
        Interview interview = interviewFinder.findById(id);
        interview.validateOwner(userId);

        if (request.getStatus() == InterviewStatus.IN_PROGRESS
                && interview.getQuestionGenerationStatus() != QuestionGenerationStatus.COMPLETED) {
            throw new BusinessException(InterviewErrorCode.QUESTION_GENERATION_NOT_COMPLETED);
        }

        try {
            interview.updateStatus(request.getStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(InterviewErrorCode.INVALID_STATUS_TRANSITION);
        }

        log.info("면접 세션 상태 변경: id={}, newStatus={}", id, request.getStatus());

        return UpdateStatusResponse.from(interview);
    }

    @Transactional
    public InterviewResponse retryQuestionGeneration(Long id, Long userId) {
        Interview interview = interviewFinder.findById(id);
        interview.validateOwner(userId);

        if (interview.getQuestionGenerationStatus() != QuestionGenerationStatus.FAILED) {
            throw new BusinessException(InterviewErrorCode.QUESTION_GENERATION_NOT_FAILED);
        }

        if (interview.getInterviewTypes().contains(InterviewType.RESUME_BASED)
                && resumeSkeletonRepository.findByInterviewId(id).isEmpty()) {
            log.warn("이력서 면접 재시도 차단 — skeleton 부재: interviewId={}, userId={}", id, userId);
            throw new BusinessException(InterviewErrorCode.RESUME_PLAN_RECOVERY_REQUIRED);
        }

        if (interview.getQuestionGenRetryCount() >= interviewProperties.retry().maxAttempts()) {
            log.warn("질문 생성 재시도 한도 초과 — interviewId={}, userId={}, count={}",
                    id, userId, interview.getQuestionGenRetryCount());
            throw new BusinessException(InterviewErrorCode.RETRY_LIMIT_EXCEEDED);
        }

        LocalDateTime lastRetriedAt = interview.getQuestionGenLastRetriedAt();
        if (lastRetriedAt != null
                && lastRetriedAt.plusSeconds(interviewProperties.retry().cooldownSeconds()).isAfter(LocalDateTime.now())) {
            log.warn("질문 생성 재시도 쿨다운 미경과 — interviewId={}, userId={}, lastRetriedAt={}",
                    id, userId, lastRetriedAt);
            throw new BusinessException(InterviewErrorCode.RETRY_COOLDOWN);
        }

        interviewRetryRecorder.record(interview);
        interview.resetForRetry();

        eventPublisher.publishEvent(new QuestionGenerationRequestedEvent(
                interview.getId(),
                userId,
                interview.getPosition(),
                interview.getPositionDetail(),
                interview.getLevel(),
                new ArrayList<>(interview.getInterviewTypes()),
                new ArrayList<>(interview.getCsSubTopics()),
                null,
                null,
                interview.getDurationMinutes(),
                interview.getTechStack()
        ));

        log.info("질문 생성 재시도 이벤트 발행: id={}, retryCount={}",
                id, interview.getQuestionGenRetryCount());

        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(id);
        return InterviewResponse.from(interview, questionSets);
    }

    @Transactional
    public void skipRemainingQuestionSets(Long id, Long userId) {
        Interview interview = interviewFinder.findById(id);
        interview.validateOwner(userId);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
        }

        questionSetService.skipRemaining(id);

        log.info("미응답 질문세트 스킵 처리: interviewId={}", id);
    }
}
