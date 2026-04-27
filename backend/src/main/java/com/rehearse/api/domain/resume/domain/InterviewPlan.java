package com.rehearse.api.domain.resume.domain;

import java.util.List;

public record InterviewPlan(
        String sessionPlanId,
        int durationHintMin,
        int totalProjects,
        List<ProjectPlan> projectPlans
) {

    public InterviewPlan {
        if (sessionPlanId == null || sessionPlanId.isBlank()) {
            throw new IllegalArgumentException("sessionPlanId 는 필수입니다.");
        }
        if (durationHintMin <= 0) {
            throw new IllegalArgumentException("durationHintMin 은 0 보다 커야 합니다. durationHintMin=" + durationHintMin);
        }
        if (projectPlans == null) {
            throw new IllegalArgumentException("projectPlans 는 필수입니다.");
        }
        if (totalProjects != projectPlans.size()) {
            throw new IllegalArgumentException(
                    "totalProjects 와 projectPlans 크기가 일치하지 않습니다. totalProjects=" + totalProjects
                            + ", projectPlans.size=" + projectPlans.size()
            );
        }
        validateAscendingPriority(projectPlans);
        projectPlans = List.copyOf(projectPlans);
    }

    // Planner 가 정렬 후 생성할 책임이 있으므로, 여기서는 순서 검증만 수행
    private static void validateAscendingPriority(List<ProjectPlan> plans) {
        for (int i = 1; i < plans.size(); i++) {
            if (plans.get(i).priority() <= plans.get(i - 1).priority()) {
                throw new IllegalArgumentException(
                        "projectPlans 의 priority 는 중복 없이 오름차순이어야 합니다."
                );
            }
        }
    }
}
