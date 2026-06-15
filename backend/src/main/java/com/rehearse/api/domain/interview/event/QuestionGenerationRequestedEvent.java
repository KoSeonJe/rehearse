package com.rehearse.api.domain.interview.event;

import com.rehearse.api.domain.interview.entity.CsSubTopic;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

import java.util.List;

public record QuestionGenerationRequestedEvent(
        Long interviewId,
        Long userId,
        Position position,
        String positionDetail,
        InterviewLevel level,
        List<InterviewType> interviewTypes,
        List<CsSubTopic> csSubTopics,
        byte[] resumePdfBytes,
        String resumeFileHash,
        Integer durationMinutes,
        TechStack techStack
) {

}
