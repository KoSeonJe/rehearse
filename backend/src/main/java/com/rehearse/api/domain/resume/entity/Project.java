package com.rehearse.api.domain.resume.entity;

import java.util.List;

public record Project(
        String projectId,
        String projectName,
        List<ResumeClaim> claims,
        List<InterrogationChain> implicitCsTopics
) {

    public Project {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId 는 필수입니다.");
        }
        claims = claims == null ? List.of() : List.copyOf(claims);
        implicitCsTopics = implicitCsTopics == null ? List.of() : List.copyOf(implicitCsTopics);
    }

    public List<ResumeClaim> claimsByPriority(Priority priority) {
        return claims.stream()
                .filter(c -> c.priority() == priority)
                .toList();
    }
}
