package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedResumeSkeleton {

    @JsonProperty("resume_id")
    private String resumeId;

    @JsonProperty("candidate_level")
    private String candidateLevel;

    @JsonProperty("target_domain")
    private String targetDomain;

    private List<ExtractedProject> projects;

    @JsonProperty("interrogation_priority_map")
    private Map<String, List<String>> interrogationPriorityMap;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedProject {

        @JsonProperty("project_id")
        private String projectId;

        private List<ExtractedClaim> claims;

        @JsonProperty("implicit_cs_topics")
        private List<ExtractedImplicitCsTopic> implicitCsTopics;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedClaim {

        @JsonProperty("claim_id")
        private String claimId;

        private String text;

        @JsonProperty("claim_type")
        private String claimType;

        private String priority;

        @JsonProperty("depth_hooks")
        private List<String> depthHooks;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedImplicitCsTopic {

        private String topic;
        private double confidence;

        @JsonProperty("interrogation_chain")
        private List<ExtractedChainStep> interrogationChain;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedChainStep {

        private int level;
        private String type;
        private String question;
    }
}
