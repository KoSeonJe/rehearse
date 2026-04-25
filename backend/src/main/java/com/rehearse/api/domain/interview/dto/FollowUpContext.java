package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.ReferenceType;

public record FollowUpContext(
        Position position,
        TechStack effectiveTechStack,
        InterviewLevel level,
        Long questionSetId,
        Long currentMainQuestionId,
        int nextOrderIndex,
        ReferenceType mainReferenceType,
        int maxFollowUpRounds
) {
}
