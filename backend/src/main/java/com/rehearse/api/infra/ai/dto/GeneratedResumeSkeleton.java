package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedResumeSkeleton(
        @JsonProperty("resume_id") String resumeId,
        @JsonProperty("candidate_level") String candidateLevel,
        @JsonProperty("target_domain") String targetDomain,
        List<GeneratedProject> projects,
        @JsonProperty("interrogation_priority_map") Map<String, List<String>> interrogationPriorityMap
) {

    public GeneratedResumeSkeleton {
        if (projects == null) {
            throw new IllegalArgumentException(
                    "GeneratedResumeSkeleton.projects 는 null 일 수 없습니다.");
        }
        projects = List.copyOf(projects);
        interrogationPriorityMap = interrogationPriorityMap != null
                ? Map.copyOf(interrogationPriorityMap)
                : Map.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedProject(
            @JsonProperty("project_id") String projectId,
            @JsonProperty("project_name") String projectName,
            @JsonProperty("tech_stack") List<String> techStack,
            String role,
            String architecture,
            List<String> decisions,
            List<GeneratedClaim> claims,
            @JsonProperty("implicit_cs_topics") List<GeneratedImplicitCsTopic> implicitCsTopics
    ) {
        public GeneratedProject {
            techStack = techStack != null ? List.copyOf(techStack) : List.of();
            decisions = decisions != null ? List.copyOf(decisions) : List.of();
            claims = claims != null ? List.copyOf(claims) : List.of();
            implicitCsTopics = implicitCsTopics != null ? List.copyOf(implicitCsTopics) : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedClaim(
            String text,
            @JsonProperty("claim_type") String claimType,
            String priority
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedImplicitCsTopic(
            String topic,
            double confidence,
            @JsonProperty("interrogation_chain") List<GeneratedChainStep> interrogationChain
    ) {
        public GeneratedImplicitCsTopic {
            interrogationChain = interrogationChain != null ? List.copyOf(interrogationChain) : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedChainStep(
            int level,
            String type,
            String question
    ) {}
}
