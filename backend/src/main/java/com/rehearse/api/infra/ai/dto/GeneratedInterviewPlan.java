package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedInterviewPlan(
        @JsonProperty("session_plan_id") String sessionPlanId,
        @JsonProperty("duration_hint_min") int durationHintMin,
        @JsonProperty("total_projects") int totalProjects,
        @JsonProperty("project_plans") List<GeneratedProjectPlan> projectPlans
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedProjectPlan(
            @JsonProperty("project_id") String projectId,
            @JsonProperty("project_name") String projectName,
            int priority,
            @JsonProperty("playground_phase") GeneratedPlaygroundPhase playgroundPhase,
            @JsonProperty("interrogation_phase") GeneratedInterrogationPhase interrogationPhase
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedPlaygroundPhase(
            @JsonProperty("opener_question") String openerQuestion,
            @JsonProperty("expected_claims_coverage") List<String> expectedClaimsCoverage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedInterrogationPhase(
            @JsonProperty("primary_chains") List<GeneratedChainRef> primaryChains,
            @JsonProperty("backup_chains") List<GeneratedChainRef> backupChains
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedChainRef(
            @JsonProperty("chain_id") String chainId,
            String topic,
            int priority,
            @JsonProperty("levels_to_cover") List<Integer> levelsToCover
    ) {}
}
