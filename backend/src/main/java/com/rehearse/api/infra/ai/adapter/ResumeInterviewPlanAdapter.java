package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
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
import java.util.Set;
import java.util.stream.Collectors;
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
        return execute(request, durationMin, null);
    }

    public InterviewPlan execute(ChatRequest request, int durationMin, ResumeSkeleton skeleton) {
        ChatResponse response = aiClient.chat(request);
        GeneratedInterviewPlan raw = aiResponseParser.parseOrRetry(
                response, GeneratedInterviewPlan.class, aiClient, request);

        Set<String> allowedChainIds = buildAllowedChainIds(skeleton);

        try {
            InterviewPlan plan = mapToDomain(raw, allowedChainIds);
            if (skeleton != null && hasMissingChain(plan, skeleton)) {
                log.warn("drop 후 chain 부족 — chain_id hallucination 재시도");
                ChatRequest retryRequest = request.withSchemaRetryHint(
                        "chain_id는 ALLOWED_CHAIN_IDS 안에서만 선택해야 합니다. 목록 외 값은 무효입니다.");
                ChatResponse retryResponse = aiClient.chat(retryRequest);
                GeneratedInterviewPlan retryRaw = aiResponseParser.parseOrRetry(
                        retryResponse, GeneratedInterviewPlan.class, aiClient, retryRequest);
                plan = mapToDomain(retryRaw, allowedChainIds);
                if (hasMissingChain(plan, skeleton)) {
                    log.error("재시도 후에도 유효한 chain 부족: skeleton projects={}", skeleton.projects().size());
                    throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
                }
            }
            return plan;
        } catch (IllegalArgumentException e) {
            log.error("LLM 출력이 도메인 invariant 위반: {}", e.getMessage());
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
    }

    private Set<String> buildAllowedChainIds(ResumeSkeleton skeleton) {
        if (skeleton == null || skeleton.projects() == null) {
            return Set.of();
        }
        return skeleton.projects().stream()
                .filter(p -> p.implicitCsTopics() != null)
                .flatMap(p -> p.implicitCsTopics().stream()
                        .map(chain -> ChainReference.synthesizeChainId(p.projectId(), chain.topic())))
                .collect(Collectors.toSet());
    }

    private boolean hasMissingChain(InterviewPlan plan, ResumeSkeleton skeleton) {
        Set<String> allowedIds = buildAllowedChainIds(skeleton);
        if (allowedIds.isEmpty()) {
            return false;
        }
        return plan.projectPlans().stream().anyMatch(pp -> {
            boolean noPrimary = pp.interrogationPhase().primaryChains().isEmpty();
            return noPrimary;
        });
    }

    private InterviewPlan mapToDomain(GeneratedInterviewPlan raw, Set<String> allowedChainIds) {
        List<ProjectPlan> projectPlans = sortByPriority(raw.projectPlans()).stream()
                .map(p -> mapProject(p, allowedChainIds))
                .toList();
        return new InterviewPlan(
                raw.sessionPlanId(),
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

    private ProjectPlan mapProject(GeneratedProjectPlan raw, Set<String> allowedChainIds) {
        return new ProjectPlan(
                raw.projectId(),
                raw.projectName(),
                raw.priority(),
                mapPlayground(raw.playgroundPhase()),
                mapInterrogation(raw.interrogationPhase(), allowedChainIds)
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

    private InterrogationPhase mapInterrogation(GeneratedInterrogationPhase raw, Set<String> allowedChainIds) {
        if (raw == null) {
            throw new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
        }
        return new InterrogationPhase(
                filterAndMapChainRefs(raw.primaryChains(), allowedChainIds),
                filterAndMapChainRefs(raw.backupChains(), allowedChainIds)
        );
    }

    private List<ChainReference> filterAndMapChainRefs(List<GeneratedChainRef> raws, Set<String> allowedChainIds) {
        if (raws == null) {
            return List.of();
        }
        return raws.stream()
                .filter(ref -> isAllowedChain(ref, allowedChainIds))
                .sorted(Comparator.comparingInt(GeneratedChainRef::priority))
                .map(this::mapChainReference)
                .toList();
    }

    private boolean isAllowedChain(GeneratedChainRef ref, Set<String> allowedChainIds) {
        if (allowedChainIds.isEmpty()) {
            return true;
        }
        if (!allowedChainIds.contains(ref.chainId())) {
            log.warn("허용되지 않은 chain_id drop: chainId={}", ref.chainId());
            return false;
        }
        return true;
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
