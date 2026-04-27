package com.rehearse.api.domain.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.entity.InterviewPlanEntity;
import com.rehearse.api.domain.resume.repository.InterviewPlanRepository;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewPlanStore {

    private final InterviewPlanRepository planRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<InterviewPlan> findByInterviewId(Long interviewId) {
        return planRepository.findByInterviewId(interviewId)
                .map(this::deserialize);
    }

    @Transactional
    public void save(Long interviewId, InterviewPlan plan) {
        String planJson = serialize(plan);
        InterviewPlanEntity entity = InterviewPlanEntity.builder()
                .interviewId(interviewId)
                .planJson(planJson)
                .build();
        try {
            planRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.warn("인터뷰 플랜 중복 저장 감지, idempotent skip: interviewId={}", interviewId);
            planRepository.findByInterviewId(interviewId)
                    .orElseThrow(() -> new BusinessException(ResumePlannerErrorCode.INVALID_PLAN));
        }
    }

    private InterviewPlan deserialize(InterviewPlanEntity entity) {
        try {
            return objectMapper.readValue(entity.getPlanJson(), InterviewPlan.class);
        } catch (JsonProcessingException e) {
            log.error("DB에서 InterviewPlan 역직렬화 실패: interviewId={}", entity.getInterviewId(), e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    private String serialize(InterviewPlan plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            log.error("InterviewPlan 직렬화 실패", e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }
}
