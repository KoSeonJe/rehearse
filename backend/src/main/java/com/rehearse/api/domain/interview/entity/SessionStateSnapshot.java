package com.rehearse.api.domain.interview.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SessionStateSnapshot(
        @JsonProperty("level") String level,
        @JsonProperty("current_turn") int currentTurn,
        @JsonProperty("covered_claims_recent") List<String> coveredClaimsRecent,
        @JsonProperty("active_chain") List<Long> activeChain,
        @JsonProperty("asked_perspectives") List<String> askedPerspectives
) {
}
