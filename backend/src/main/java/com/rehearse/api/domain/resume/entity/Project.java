package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Project(
        String projectId,
        String projectName,
        List<String> techStack,
        String role,
        String architecture,
        List<String> decisions,
        List<ResumeClaim> claims,
        List<InterrogationChain> implicitCsTopics
) {

    public Project {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId 는 필수입니다.");
        }
        techStack = techStack == null ? List.of() : List.copyOf(techStack);
        role = role == null ? "" : role;
        architecture = architecture == null ? "" : architecture;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        claims = claims == null ? List.of() : List.copyOf(claims);
        implicitCsTopics = implicitCsTopics == null ? List.of() : List.copyOf(implicitCsTopics);
    }

    public List<ResumeClaim> claimsByPriority(Priority priority) {
        return claims.stream()
                .filter(c -> c.priority() == priority)
                .toList();
    }
}
