package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InterviewListResponse {

    private final Long id;
    private final String publicId;
    private final Position position;
    private final String positionDetail;
    private final List<InterviewType> interviewTypes;
    private final List<String> csSubTopics;
    private final Integer durationMinutes;
    private final InterviewStatus status;
    private final long answerCount;
    private final LocalDateTime createdAt;
}
