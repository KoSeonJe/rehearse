package com.rehearse.api.domain.interview.service.event;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuestionGenerationRequestedEvent {

    private final Long interviewId;
    private final Long userId;
    private final Position position;
    private final String positionDetail;
    private final InterviewLevel level;
    private final List<InterviewType> interviewTypes;
    private final List<String> csSubTopics;
    private final String resumeText;
    private final Integer durationMinutes;
    private final TechStack techStack;
}
