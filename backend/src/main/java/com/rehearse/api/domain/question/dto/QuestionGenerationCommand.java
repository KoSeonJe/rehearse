package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

import java.util.List;

public record QuestionGenerationCommand(
        Long interviewId,
        Long userId,
        Position position,
        InterviewLevel level,
        List<InterviewType> interviewTypes,
        List<String> csSubTopics,
        String resumeText,
        Integer durationMinutes,
        TechStack techStack
) {

    public GetQuestionPoolCommand toGetQuestionPoolCommand() {
        return new GetQuestionPoolCommand(
                userId,
                position,
                level,
                techStack,
                csSubTopics
        );
    }
}
