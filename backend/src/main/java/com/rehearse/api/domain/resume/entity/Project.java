package com.rehearse.api.domain.resume.entity;

import java.util.List;

public record Project(
        String projectId,
        String projectName,
        List<ResumeClaim> claims,
        List<InterrogationChain> implicitCsTopics
) {

    public Project {
        claims = claims == null ? List.of() : List.copyOf(claims);
        implicitCsTopics = implicitCsTopics == null ? List.of() : List.copyOf(implicitCsTopics);
        projectName = (projectName == null || projectName.isBlank()) ? projectId : projectName;
    }

    public String displayName() {
        return projectName;
    }

    public List<ResumeClaim> claimsByPriority(Priority priority) {
        return claims.stream()
                .filter(c -> c.priority() == priority)
                .toList();
    }
}
