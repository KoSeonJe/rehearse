package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.models.service.FollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpQuestionService {

    private final FollowUpQuestionGenerator followUpQuestionGenerator;

    public GeneratedFollowUp write(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis
    ) {
        return followUpQuestionGenerator.generate(mainQuestion, userAnswer, analysis);
    }
}
