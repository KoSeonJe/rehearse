package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedChainRef;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedInterrogationPhase;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedPlaygroundPhase;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedProjectPlan;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeInterviewPlanAdapter {

    private static final List<Integer> DEFAULT_LEVELS = List.of(1, 2, 3, 4);

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    public InterviewPlan execute(ChatRequest request, int durationMin) {
        ChatResponse response = aiClient.chat(request);
        GeneratedInterviewPlan raw = aiResponseParser.parseOrRetry(
                response, GeneratedInterviewPlan.class, aiClient, request);

        if (raw.durationHintMin() != durationMin) {
            log.warn("LLM이 duration_hint_min을 임의 변경함. 강제 덮어쓰기: expected={}, actual={}",
                    durationMin, raw.durationHintMin());
        }

        try {
            return mapToDomain(raw, durationMin);
        } catch (IllegalArgumentException e) {
            log.error("LLM 출력이 도메인 invariant 위반: {}", e.getMessage());
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
    }

    private InterviewPlan mapToDomain(GeneratedInterviewPlan raw, int durationMin) {
        List<ProjectPlan> projectPlans = sortByPriority(raw.projectPlans()).stream()
                .map(this::mapProject)
                .toList();
        return new InterviewPlan(
                raw.sessionPlanId(),
                durationMin,
                projectPlans
        );
    }

    private List<GeneratedProjectPlan> sortByPriority(List<GeneratedProjectPlan> projectPlans) {
        if (projectPlans == null) {
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
        return projectPlans.stream()
                .sorted(Comparator.comparingInt(GeneratedProjectPlan::priority))
                .toList();
    }

    private ProjectPlan mapProject(GeneratedProjectPlan raw) {
        return new ProjectPlan(
                raw.projectId(),
                raw.projectName(),
                raw.priority(),
                mapPlayground(raw.playgroundPhase()),
                mapInterrogation(raw.interrogationPhase())
        );
    }

    private PlaygroundPhase mapPlayground(GeneratedPlaygroundPhase raw) {
        if (raw == null) {
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
        return new PlaygroundPhase(
                raw.openerQuestion(),
                raw.expectedClaimsCoverage() != null ? raw.expectedClaimsCoverage() : List.of()
        );
    }

    private InterrogationPhase mapInterrogation(GeneratedInterrogationPhase raw) {
        if (raw == null) {
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
        return new InterrogationPhase(
                mapChainReferenceerences(raw.primaryChains()),
                mapChainReferenceerences(raw.backupChains())
        );
    }

    private List<ChainReference> mapChainReferenceerences(List<GeneratedChainRef> raws) {
        if (raws == null) {
            return List.of();
        }
        return raws.stream()
                .sorted(Comparator.comparingInt(GeneratedChainRef::priority))
                .map(this::mapChainReference)
                .toList();
    }

    private ChainReference mapChainReference(GeneratedChainRef raw) {
        return new ChainReference(
                raw.chainId(),
                raw.topic(),
                raw.priority(),
                raw.levelsToCover() != null ? raw.levelsToCover() : DEFAULT_LEVELS
        );
    }
}
