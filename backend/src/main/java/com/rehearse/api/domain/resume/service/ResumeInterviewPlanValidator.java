package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResumeInterviewPlanValidator {

    public void validate(ResumeSkeleton skeleton, InterviewPlan plan) {
        Set<String> chainIds = collectChainIds(skeleton);
        Map<String, Set<String>> claimIdsByProject = collectClaimIdsByProject(skeleton);

        for (ProjectPlan projectPlan : plan.projectPlans()) {
            validateChainReferences(projectPlan, chainIds);
            validateClaimCoverage(projectPlan, claimIdsByProject);
        }
    }

    private Set<String> collectChainIds(ResumeSkeleton skeleton) {
        return skeleton.projects().stream()
                .flatMap(p -> p.implicitCsTopics().stream()
                        .map(chain -> ChainReference.synthesizeChainId(p.projectId(), chain.topic())))
                .collect(Collectors.toSet());
    }

    private Map<String, Set<String>> collectClaimIdsByProject(ResumeSkeleton skeleton) {
        return skeleton.projects().stream()
                .collect(Collectors.toMap(
                        Project::projectId,
                        p -> p.claims().stream().map(ResumeClaim::claimId).collect(Collectors.toSet())
                ));
    }

    private void validateChainReferences(ProjectPlan projectPlan, Set<String> chainIds) {
        projectPlan.interrogationPhase().primaryChains().forEach(c -> assertChainExists(c, chainIds));
        projectPlan.interrogationPhase().backupChains().forEach(c -> assertChainExists(c, chainIds));
    }

    private void assertChainExists(ChainReference chain, Set<String> chainIds) {
        if (!chainIds.contains(chain.chainId())) {
            log.error("Plan에 Skeleton에 없는 chain_id 참조: chainId={}", chain.chainId());
            throw new BusinessException(ResumePlannerErrorCode.ORPHAN_CHAIN);
        }
    }

    private void validateClaimCoverage(ProjectPlan projectPlan, Map<String, Set<String>> claimIdsByProject) {
        Set<String> validClaims = claimIdsByProject.getOrDefault(projectPlan.projectId(), Set.of());
        for (String claimId : projectPlan.playgroundPhase().expectedClaimsCoverage()) {
            if (!validClaims.contains(claimId)) {
                log.error("Plan에 Skeleton에 없는 claim 참조: projectId={}, claimId={}",
                        projectPlan.projectId(), claimId);
                throw new BusinessException(ResumePlannerErrorCode.ORPHAN_CLAIM);
            }
        }
    }
}
