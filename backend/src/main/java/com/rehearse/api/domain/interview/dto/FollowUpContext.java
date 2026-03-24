package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

public record FollowUpContext(
        Position position,
        TechStack effectiveTechStack,
        InterviewLevel level,
        Long questionSetId,
        int nextOrderIndex
) {
}
