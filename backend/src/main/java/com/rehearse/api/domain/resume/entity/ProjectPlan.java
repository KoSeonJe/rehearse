package com.rehearse.api.domain.resume.entity;

public record ProjectPlan(
        String projectId,
        String projectName,
        int priority,
        PlaygroundPhase playgroundPhase,
        InterrogationPhase interrogationPhase
) {

    private static final int MIN_PRIORITY = 1;

    public ProjectPlan {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId 는 필수입니다.");
        }
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("projectName 은 필수입니다.");
        }
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("priority 는 1 이상이어야 합니다. priority=" + priority);
        }
        if (playgroundPhase == null) {
            throw new IllegalArgumentException("playgroundPhase 는 필수입니다.");
        }
        if (interrogationPhase == null) {
            throw new IllegalArgumentException("interrogationPhase 는 필수입니다.");
        }
    }
}
