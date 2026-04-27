package com.rehearse.api.domain.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.ChainRef;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
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
import com.rehearse.api.infra.ai.prompt.ResumeInterviewPlannerPromptBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeInterviewPlanner {

    private static final String CALL_TYPE = "resume_interview_planner";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final ResumeInterviewPlannerPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public InterviewPlan plan(ResumeSkeleton skeleton, int durationMin) {
        String skeletonJson = serializeSkeleton(skeleton);
        String userLevel = skeleton.candidateLevel() != null ? skeleton.candidateLevel().name() : "MID";
        ChatRequest request = promptBuilder.build(skeletonJson, durationMin, userLevel, CALL_TYPE);
        ChatResponse response = aiClient.chat(request);

        GeneratedInterviewPlan raw = aiResponseParser.parseOrRetry(
                response, GeneratedInterviewPlan.class, aiClient, request);

        if (raw.durationHintMin() != durationMin) {
            log.warn("LLM이 duration_hint_min을 임의 변경함. 강제 덮어쓰기: expected={}, actual={}",
                    durationMin, raw.durationHintMin());
        }

        Set<String> skeletonChainIds = buildSkeletonChainIds(skeleton);
        validateChainIds(raw, skeletonChainIds);

        Set<String> skeletonClaimIdsByProject = null; // per-project 검증은 아래에서 직접 수행
        validateClaimCoverage(raw, skeleton);

        List<GeneratedProjectPlan> sorted = sortByPriority(raw.projectPlans());

        InterviewPlan plan = toDomain(raw, durationMin, sorted);
        log.info("인터뷰 플랜 생성 완료: sessionPlanId={}, projects={}, durationHintMin={}",
                plan.sessionPlanId(), plan.totalProjects(), plan.durationHintMin());
        return plan;
    }

    // skeleton의 모든 chain을 "${projectId}::${topic}" 합성키 set으로 구성
    private Set<String> buildSkeletonChainIds(ResumeSkeleton skeleton) {
        return skeleton.projects().stream()
                .flatMap(p -> p.implicitCsTopics().stream()
                        .map(chain -> p.projectId() + "::" + chain.topic()))
                .collect(Collectors.toSet());
    }

    private void validateChainIds(GeneratedInterviewPlan raw, Set<String> skeletonChainIds) {
        if (raw.projectPlans() == null) {
            return;
        }
        for (GeneratedProjectPlan projectPlan : raw.projectPlans()) {
            if (projectPlan.interrogationPhase() == null) {
                continue;
            }
            validateChainList(projectPlan.interrogationPhase().primaryChains(), skeletonChainIds);
            validateChainList(projectPlan.interrogationPhase().backupChains(), skeletonChainIds);
        }
    }

    private void validateChainList(List<GeneratedChainRef> chains, Set<String> skeletonChainIds) {
        if (chains == null) {
            return;
        }
        for (GeneratedChainRef chain : chains) {
            if (chain.chainId() == null || !chain.chainId().contains("::")) {
                throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
            }
            if (!skeletonChainIds.contains(chain.chainId())) {
                log.error("Plan에 Skeleton에 없는 chain_id 참조: chainId={}", chain.chainId());
                throw new BusinessException(ResumePlannerErrorCode.ORPHAN_CHAIN);
            }
        }
    }

    private void validateClaimCoverage(GeneratedInterviewPlan raw, ResumeSkeleton skeleton) {
        if (raw.projectPlans() == null) {
            return;
        }
        for (GeneratedProjectPlan projectPlan : raw.projectPlans()) {
            if (projectPlan.playgroundPhase() == null
                    || projectPlan.playgroundPhase().expectedClaimsCoverage() == null) {
                continue;
            }
            Set<String> validClaimIds = findClaimIds(skeleton, projectPlan.projectId());
            for (String claimId : projectPlan.playgroundPhase().expectedClaimsCoverage()) {
                if (!validClaimIds.contains(claimId)) {
                    log.error("Plan에 Skeleton에 없는 claim 참조: projectId={}, claimId={}",
                            projectPlan.projectId(), claimId);
                    throw new BusinessException(ResumePlannerErrorCode.ORPHAN_CLAIM);
                }
            }
        }
    }

    private Set<String> findClaimIds(ResumeSkeleton skeleton, String projectId) {
        return skeleton.projects().stream()
                .filter(p -> p.projectId().equals(projectId))
                .findFirst()
                .map(Project::claims)
                .orElse(List.of())
                .stream()
                .map(claim -> claim.claimId())
                .collect(Collectors.toSet());
    }

    private List<GeneratedProjectPlan> sortByPriority(List<GeneratedProjectPlan> projectPlans) {
        if (projectPlans == null) {
            return List.of();
        }
        return projectPlans.stream()
                .sorted(Comparator.comparingInt(GeneratedProjectPlan::priority))
                .toList();
    }

    private InterviewPlan toDomain(GeneratedInterviewPlan raw, int durationMin,
            List<GeneratedProjectPlan> sorted) {
        List<ProjectPlan> projectPlans = sorted.stream()
                .map(this::toDomain)
                .toList();
        return new InterviewPlan(
                raw.sessionPlanId(),
                durationMin,
                raw.totalProjects(),
                projectPlans
        );
    }

    private ProjectPlan toDomain(GeneratedProjectPlan raw) {
        return new ProjectPlan(
                raw.projectId(),
                raw.projectName(),
                raw.priority(),
                toDomain(raw.playgroundPhase()),
                toDomain(raw.interrogationPhase())
        );
    }

    private PlaygroundPhase toDomain(GeneratedPlaygroundPhase raw) {
        if (raw == null) {
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
        return new PlaygroundPhase(
                raw.openerQuestion(),
                raw.expectedClaimsCoverage() != null ? raw.expectedClaimsCoverage() : List.of()
        );
    }

    private InterrogationPhase toDomain(GeneratedInterrogationPhase raw) {
        if (raw == null) {
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
        List<ChainRef> primary = toChainRefs(raw.primaryChains());
        List<ChainRef> backup = toChainRefs(raw.backupChains());
        return new InterrogationPhase(primary, backup);
    }

    private List<ChainRef> toChainRefs(List<GeneratedChainRef> raws) {
        if (raws == null) {
            return List.of();
        }
        return raws.stream()
                .map(r -> new ChainRef(
                        r.chainId(),
                        r.topic(),
                        r.priority(),
                        r.levelsToCover() != null ? r.levelsToCover() : List.of(1, 2, 3, 4)
                ))
                .toList();
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
