package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

import java.util.Set;

public record QuestionGenerationRequest(
    Position position,
    String positionDetail,
    InterviewLevel level,
    Set<InterviewType> interviewTypes,
    Set<String> csSubTopics,
    String resumeText,
    Integer durationMinutes,
    TechStack techStack
) {}
