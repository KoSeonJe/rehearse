package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    private final VocalFeedback vocal;
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

    @Getter
    @Builder
    public static class VocalFeedback {
        private final List<String> fillerWords;
        private final String speechPace;        // "빠름" / "적절" / "느림"
        private final Integer toneConfidence;   // 0-100
        private final String emotionLabel;      // "자신감" / "긴장" / "평온" / "불안"
        private final String vocalComment;
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

        VocalFeedback vocal = VocalFeedback.builder()
                .fillerWords(feedback.getFillerWords())
                .speechPace(feedback.getSpeechPace())
                .toneConfidence(feedback.getToneConfidence())
                .emotionLabel(feedback.getEmotionLabel())
                .vocalComment(feedback.getVocalComment())
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
                .vocal(vocal)
                .isAnalyzed(feedback.isAnalyzed())
                .build();
    }
}
