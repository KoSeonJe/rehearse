package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.dto.QuestionGenerationCommand;
import com.rehearse.api.domain.question.entity.QuestionSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final ResumeTrackInitiator resumeTrackInitiator;
    private final StandardTrackQuestionGenerator standardTrackGenerator;

    public void generateQuestions(Long interviewId, Long userId, Position position,
                                  InterviewLevel level, List<InterviewType> interviewTypes,
                                  List<String> csSubTopics, String resumeText,
                                  Integer durationMinutes, TechStack techStack) {
        generateQuestions(interviewId, userId, position, level, interviewTypes, csSubTopics,
                resumeText, null, durationMinutes, techStack);
    }

    public void generateQuestions(Long interviewId, Long userId, Position position,
                                  InterviewLevel level, List<InterviewType> interviewTypes,
                                  List<String> csSubTopics, String resumeText, String resumeFileHash,
                                  Integer durationMinutes, TechStack techStack) {

        transactionHandler.startGeneration(interviewId);

        if (interviewTypes.contains(InterviewType.RESUME_BASED)) {
            resumeTrackInitiator.initiate(interviewId, level, resumeFileHash, resumeText, durationMinutes);
            return;
        }

        List<QuestionSet> questionSets = standardTrackGenerator.generate(new QuestionGenerationCommand(
                interviewId, userId, position, level, interviewTypes,
                csSubTopics, resumeText, durationMinutes, techStack));

        transactionHandler.saveResults(interviewId, questionSets);
    }
}
