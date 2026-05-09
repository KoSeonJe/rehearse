package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeClaim(
        String text,
        ClaimType claimType,
        Priority priority
) {

    public ResumeClaim {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("ResumeClaim.text 는 필수입니다.");
        }
        if (claimType == null) {
            throw new IllegalArgumentException("ResumeClaim.claimType 는 필수입니다.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("ResumeClaim.priority 는 필수입니다.");
        }
    }
}
