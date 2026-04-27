package com.rehearse.api.domain.resume.domain;

import java.util.List;

public record ResumeClaim(
        String claimId,
        String text,
        ClaimType claimType,
        Priority priority,
        List<String> depthHooks
) {
}
