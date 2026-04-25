package com.rehearse.api.domain.interview;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Claim(
        String text,
        int depthScore,
        EvidenceStrength evidenceStrength,
        String topicTag
) {

    @JsonCreator
    public Claim(
            @JsonProperty("text") String text,
            @JsonProperty("depth_score") int depthScore,
            @JsonProperty("evidence_strength") EvidenceStrength evidenceStrength,
            @JsonProperty("topic_tag") String topicTag
    ) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Claim.text 는 비어있을 수 없습니다.");
        }
        if (depthScore < 1 || depthScore > 5) {
            throw new IllegalArgumentException("Claim.depthScore 는 1~5 범위여야 합니다: " + depthScore);
        }
        if (evidenceStrength == null) {
            throw new IllegalArgumentException("Claim.evidenceStrength 는 null 일 수 없습니다.");
        }
        this.text = text;
        this.depthScore = depthScore;
        this.evidenceStrength = evidenceStrength;
        this.topicTag = topicTag;
    }
}
