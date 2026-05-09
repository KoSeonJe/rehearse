package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResumeInterviewPlanValidator {

    private static final double JACCARD_THRESHOLD = 0.6;

    public void validate(ResumeSkeleton skeleton, InterviewPlan plan) {
        Set<String> chainIds = collectChainIds(skeleton);
        Map<String, Set<String>> claimTextsByProject = collectClaimTextsByProject(skeleton);

        int droppedClaimCount = 0;
        for (ProjectPlan projectPlan : plan.projectPlans()) {
            validateChainReferences(projectPlan, chainIds);
            droppedClaimCount += countDroppedClaims(projectPlan, claimTextsByProject);
        }

        if (droppedClaimCount > 0) {
            log.warn("Plan claim coverage graceful drop 발생 droppedClaimCount={}", droppedClaimCount);
        }
    }

    private Set<String> collectChainIds(ResumeSkeleton skeleton) {
        return skeleton.projects().stream()
                .flatMap(p -> p.implicitCsTopics().stream()
                        .map(chain -> ChainReference.synthesizeChainId(p.projectId(), chain.topic())))
                .collect(Collectors.toSet());
    }

    private Map<String, Set<String>> collectClaimTextsByProject(ResumeSkeleton skeleton) {
        return skeleton.projects().stream()
                .collect(Collectors.toMap(
                        Project::projectId,
                        p -> p.claims().stream().map(ResumeClaim::text).collect(Collectors.toSet())
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

    private int countDroppedClaims(ProjectPlan projectPlan, Map<String, Set<String>> claimTextsByProject) {
        Set<String> skeletonClaimTexts = claimTextsByProject.getOrDefault(projectPlan.projectId(), Set.of());
        int dropped = 0;
        for (String expected : projectPlan.playgroundPhase().expectedClaimsCoverage()) {
            if (!matchesAny(expected, skeletonClaimTexts)) {
                dropped++;
                log.warn("Plan claim coverage 매칭 실패 → drop: projectId={} expected={}",
                        projectPlan.projectId(), expected);
            }
        }
        return dropped;
    }

    private boolean matchesAny(String expected, Set<String> skeletonClaimTexts) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        if (skeletonClaimTexts.contains(expected)) {
            return true;
        }
        for (String claimText : skeletonClaimTexts) {
            if (claimText.contains(expected) || expected.contains(claimText)) {
                return true;
            }
            if (jaccardSimilarity(expected, claimText) >= JACCARD_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private double jaccardSimilarity(String a, String b) {
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("\\s+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());
    }
}
