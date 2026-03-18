package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimestampFeedbackResponse {

    private final Long id;
    private final Long questionId;
    private final String questionType;
    private final long startMs;
    private final long endMs;
    private final String transcript;
    private final Integer verbalScore;
    private final String verbalComment;
    private final Integer fillerWordCount;
    private final Integer eyeContactScore;
    private final Integer postureScore;
    private final String expressionLabel;
    private final String nonverbalComment;
    private final String overallComment;
    private final boolean isAnalyzed;

    public static TimestampFeedbackResponse from(TimestampFeedback feedback) {
        Question question = feedback.getQuestion();
        return TimestampFeedbackResponse.builder()
                .id(feedback.getId())
                .questionId(question != null ? question.getId() : null)
                .questionType(question != null ? question.getQuestionType().name() : null)
                .startMs(feedback.getStartMs())
                .endMs(feedback.getEndMs())
                .transcript(feedback.getTranscript())
                .verbalScore(feedback.getVerbalScore())
                .verbalComment(feedback.getVerbalComment())
                .fillerWordCount(feedback.getFillerWordCount())
                .eyeContactScore(feedback.getEyeContactScore())
                .postureScore(feedback.getPostureScore())
                .expressionLabel(feedback.getExpressionLabel())
                .nonverbalComment(feedback.getNonverbalComment())
                .overallComment(feedback.getOverallComment())
                .isAnalyzed(feedback.isAnalyzed())
                .build();
    }
}
