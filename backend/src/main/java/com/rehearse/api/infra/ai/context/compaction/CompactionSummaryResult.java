package com.rehearse.api.infra.ai.context.compaction;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CompactionSummaryResult(
        @JsonProperty("covered_topics") List<String> coveredTopics,
        @JsonProperty("user_claims_made") List<String> userClaimsMade,
        @JsonProperty("chain_progress_history") List<String> chainProgressHistory,
        @JsonProperty("perspectives_asked") List<String> perspectivesAsked,
        @JsonProperty("notable_moments") List<String> notableMoments
) {
    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "topics", coveredTopics);
        appendSection(sb, "claims", userClaimsMade);
        appendSection(sb, "chain", chainProgressHistory);
        appendSection(sb, "perspectives", perspectivesAsked);
        appendSection(sb, "moments", notableMoments);
        return sb.toString().trim();
    }

    private void appendSection(StringBuilder sb, String label, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append("[").append(label).append("] ").append(String.join(" / ", items)).append("\n");
    }
}
