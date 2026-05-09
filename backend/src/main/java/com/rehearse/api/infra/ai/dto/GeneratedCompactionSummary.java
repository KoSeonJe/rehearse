package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedCompactionSummary(
        @JsonProperty("covered_topics") List<String> coveredTopics,
        @JsonProperty("user_claims_made") List<String> userClaimsMade,
        @JsonProperty("chain_progress_history") List<String> chainProgressHistory,
        @JsonProperty("perspectives_asked") List<String> perspectivesAsked,
        @JsonProperty("notable_moments") List<String> notableMoments
) {

    public GeneratedCompactionSummary {
        coveredTopics = coveredTopics != null ? List.copyOf(coveredTopics) : List.of();
        userClaimsMade = userClaimsMade != null ? List.copyOf(userClaimsMade) : List.of();
        chainProgressHistory = chainProgressHistory != null ? List.copyOf(chainProgressHistory) : List.of();
        perspectivesAsked = perspectivesAsked != null ? List.copyOf(perspectivesAsked) : List.of();
        notableMoments = notableMoments != null ? List.copyOf(notableMoments) : List.of();
    }

    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "topics", coveredTopics);
        appendSection(sb, "claims", userClaimsMade);
        appendSection(sb, "chain", chainProgressHistory);
        appendSection(sb, "perspectives", perspectivesAsked);
        appendSection(sb, "moments", notableMoments);
        return sb.toString().trim();
    }

    private static void appendSection(StringBuilder sb, String label, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append("[").append(label).append("] ").append(String.join(" / ", items)).append("\n");
    }
}
