package com.rehearse.api.domain.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.ResumeInterviewPlanAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.prompt.ResumeInterviewPlannerPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeInterviewPlanner {

    private static final String CALL_TYPE = "resume_interview_planner";
    private static final String DEFAULT_USER_LEVEL = "MID";

    private final ResumeInterviewPlannerPromptBuilder promptBuilder;
    private final ResumeInterviewPlanAdapter planAdapter;
    private final ResumeInterviewPlanValidator planValidator;
    private final ObjectMapper objectMapper;

    public InterviewPlan plan(ResumeSkeleton skeleton, int durationMin) {
        ChatRequest request = buildRequest(skeleton, durationMin);
        InterviewPlan plan = planAdapter.execute(request, durationMin);
        planValidator.validate(skeleton, plan);
        log.info("인터뷰 플랜 생성 완료: sessionPlanId={}, projects={}, durationHintMin={}",
                plan.sessionPlanId(), plan.totalProjects(), plan.durationHintMin());
        return plan;
    }

    private ChatRequest buildRequest(ResumeSkeleton skeleton, int durationMin) {
        String skeletonJson = serializeSkeleton(skeleton);
        String userLevel = skeleton.candidateLevel() != null
                ? skeleton.candidateLevel().name()
                : DEFAULT_USER_LEVEL;
        return promptBuilder.build(skeletonJson, durationMin, userLevel, CALL_TYPE);
    }

    private String serializeSkeleton(ResumeSkeleton skeleton) {
        try {
            return objectMapper.writeValueAsString(skeleton);
        } catch (JsonProcessingException e) {
            log.error("ResumeSkeleton 직렬화 실패", e);
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
    }
}
