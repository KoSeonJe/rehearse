package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
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

    public InterviewPlan plan(ResumeSkeleton skeleton, int durationMin) {
        ChatRequest request = buildRequest(skeleton, durationMin);
        InterviewPlan plan = planAdapter.execute(request, durationMin, skeleton);
        planValidator.validate(skeleton, plan);
        log.info("인터뷰 플랜 생성 완료: sessionPlanId={}, projects={}", plan.sessionPlanId(), plan.totalProjects());
        return plan;
    }

    private ChatRequest buildRequest(ResumeSkeleton skeleton, int durationMin) {
        String userLevel = skeleton.candidateLevel() != null
                ? skeleton.candidateLevel().name()
                : DEFAULT_USER_LEVEL;
        return promptBuilder.build(skeleton, durationMin, userLevel, CALL_TYPE);
    }
}
