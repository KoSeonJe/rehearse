package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.ReferenceType;

import java.util.List;

public record FollowUpGenerationRequest(
    Position position,
    TechStack techStack,
    InterviewLevel level,
    String questionContent,
    String answerText,
    String nonVerbalSummary,
    List<FollowUpRequest.FollowUpExchange> previousExchanges,
    ReferenceType mainReferenceType
) {}
