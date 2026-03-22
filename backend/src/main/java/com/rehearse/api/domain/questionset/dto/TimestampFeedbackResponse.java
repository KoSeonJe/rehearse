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
    private final String questionText;
    private final String modelAnswer;
    private final long startMs;
    private final long endMs;
    private final String transcript;
    private final TechnicalFeedback technical;
    private final NonverbalFeedback nonverbal;
    private final boolean isAnalyzed;

    @Getter
    @Builder
    public static class TechnicalFeedback {
        private final Integer verbalScore;
        private final String verbalComment;
        private final Integer fillerWordCount;
    }

    @Getter
    @Builder
    public static class NonverbalFeedback {
        private final Integer eyeContactScore;
        private final Integer postureScore;
        private final String expressionLabel;
        private final String nonverbalComment;
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback) {
        Question question = feedback.getQuestion();

        TechnicalFeedback technical = TechnicalFeedback.builder()
                .verbalScore(feedback.getVerbalScore())
                .verbalComment(feedback.getVerbalComment())
                .fillerWordCount(feedback.getFillerWordCount())
                .build();

        NonverbalFeedback nonverbal = NonverbalFeedback.builder()
                .eyeContactScore(feedback.getEyeContactScore())
                .postureScore(feedback.getPostureScore())
                .expressionLabel(feedback.getExpressionLabel())
                .nonverbalComment(feedback.getNonverbalComment())
                .build();

        return TimestampFeedbackResponse.builder()
                .id(feedback.getId())
                .questionId(question != null ? question.getId() : null)
                .questionType(question != null ? question.getQuestionType().name() : null)
                .questionText(question != null ? question.getQuestionText() : null)
                .modelAnswer(question != null ? question.getModelAnswer() : null)
                .startMs(feedback.getStartMs())
                .endMs(feedback.getEndMs())
                .transcript(feedback.getTranscript())
                .technical(technical)
                .nonverbal(nonverbal)
                .isAnalyzed(feedback.isAnalyzed())
                .build();
    }
}
