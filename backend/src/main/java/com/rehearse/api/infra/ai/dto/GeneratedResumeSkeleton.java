package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedResumeSkeleton(
        @JsonProperty("resume_id") String resumeId,
        @JsonProperty("candidate_level") String candidateLevel,
        @JsonProperty("target_domain") String targetDomain,
        List<GeneratedProject> projects
) {

    public GeneratedResumeSkeleton {
        projects = projects != null ? List.copyOf(projects) : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedProject(
            @JsonProperty("project_id") String projectId,
            @JsonProperty("project_name") String projectName,
            @JsonProperty("tech_stack") List<String> techStack,
            String role,
            String architecture,
            List<String> decisions
    ) {
        public GeneratedProject {
            techStack = techStack != null ? List.copyOf(techStack) : List.of();
            decisions = decisions != null ? List.copyOf(decisions) : List.of();
        }
    }
}
