package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import java.util.List;

public record GetQuestionPoolCommand(
        Long userId,
        Position position,
        InterviewLevel level,
        TechStack techStack,
        List<String> csSubTopics
) {

}
