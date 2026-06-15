package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Project(
        String projectId,
        String projectName,
        List<String> techStack,
        String role,
        String architecture,
        List<String> decisions,
        @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = DepthSignals.EmptyValueFilter.class)
        DepthSignals depthSignals
) {

    public Project {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId 는 필수입니다.");
        }
        techStack = techStack == null ? List.of() : List.copyOf(techStack);
        role = role == null ? "" : role;
        architecture = architecture == null ? "" : architecture;
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        depthSignals = depthSignals == null ? DepthSignals.empty() : depthSignals;
    }
}
