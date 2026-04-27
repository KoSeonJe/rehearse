package com.rehearse.api.domain.resume.domain;

import java.util.List;

public record PlaygroundPhase(
        String openerQuestion,
        List<String> expectedClaimsCoverage
) {

    public PlaygroundPhase {
        if (openerQuestion == null || openerQuestion.isBlank()) {
            throw new IllegalArgumentException("openerQuestion 은 필수입니다.");
        }
        expectedClaimsCoverage = expectedClaimsCoverage == null
                ? List.of()
                : List.copyOf(expectedClaimsCoverage);
    }
}
