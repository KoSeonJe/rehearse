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
            List<String> decisions,
            @JsonProperty("depth_signals") GeneratedDepthSignals depthSignals
    ) {
        public GeneratedProject {
            techStack = techStack != null ? List.copyOf(techStack) : List.of();
            decisions = decisions != null ? List.copyOf(decisions) : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedDepthSignals(
            List<String> tradeoffs,
            List<String> alternatives,
            List<String> quantitative,
            @JsonProperty("decision_rationale") List<String> decisionRationale
    ) {
        public GeneratedDepthSignals {
            tradeoffs = tradeoffs != null ? List.copyOf(tradeoffs) : List.of();
            alternatives = alternatives != null ? List.copyOf(alternatives) : List.of();
            quantitative = quantitative != null ? List.copyOf(quantitative) : List.of();
            decisionRationale = decisionRationale != null ? List.copyOf(decisionRationale) : List.of();
        }
    }
}
