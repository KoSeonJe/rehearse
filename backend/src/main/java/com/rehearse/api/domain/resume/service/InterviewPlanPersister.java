package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.domain.resume.repository.InterviewPlanRepository;
import com.rehearse.api.global.exception.BusinessException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewPlanPersister {

    private final InterviewPlanRepository planRepository;

    @Transactional(readOnly = true)
    public Optional<InterviewPlan> findByInterviewId(Long interviewId) {
        return planRepository.findByInterviewId(interviewId);
    }

    @Transactional
    public void save(Long interviewId, InterviewPlan plan) {
        plan.assignToInterview(interviewId);
        try {
            planRepository.save(plan);
        } catch (DataIntegrityViolationException e) {
            log.warn("인터뷰 플랜 중복 저장 감지, idempotent skip: interviewId={}", interviewId);
            planRepository.findByInterviewId(interviewId)
                    .orElseThrow(() -> new BusinessException(ResumePlannerErrorCode.INVALID_PLAN));
        }
    }
}
