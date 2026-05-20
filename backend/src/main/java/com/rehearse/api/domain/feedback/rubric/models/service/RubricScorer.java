package com.rehearse.api.domain.feedback.rubric.models.service;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;

import java.util.List;

public interface RubricScorer {

    RubricScoringResult score(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel level,
            Long interviewId,
            Long questionId
    );
}
