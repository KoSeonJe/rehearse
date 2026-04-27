package com.rehearse.api.domain.resume.domain;

import java.util.List;

public record Project(
        String projectId,
        List<ResumeClaim> claims,
        List<InterrogationChain> implicitCsTopics
) {

    public Project {
        claims = claims == null ? List.of() : List.copyOf(claims);
        implicitCsTopics = implicitCsTopics == null ? List.of() : List.copyOf(implicitCsTopics);
    }

    public List<ResumeClaim> claimsByPriority(Priority priority) {
        return claims.stream()
                .filter(c -> c.priority() == priority)
                .toList();
    }
}
