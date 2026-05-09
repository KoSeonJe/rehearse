package com.rehearse.api.domain.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.question.entity.Question;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class TimestampFeedbackResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Long id;
    private final Long questionId;
    private final String questionType;
    private final String questionText;
    private final String bestAnswer;
    private final long startMs;
    private final long endMs;
    private final String transcript;
    private final DeliveryFeedback delivery;
    private final TechnicalFeedback technicalFeedback;
    private final CommentBlock overallComment;
    @JsonProperty("isAnalyzed")
    private final boolean isAnalyzed;

    @Getter
    @Builder
    @Jacksonized
    public static class CommentBlock {
        private final String positive;
        private final String negative;
        private final String suggestion;
    }

    @Getter
    @Builder
    public static class DeliveryFeedback {
        private final NonverbalFeedback nonverbal;
        private final VocalFeedback vocal;
        private final CommentBlock attitudeComment;
    }

    @Getter
    @Builder
    public static class NonverbalFeedback {
        private final String eyeContactLevel;
        private final String postureLevel;
        private final String expressionLabel;
        private final CommentBlock nonverbalComment;
    }

    @Getter
    @Builder
    public static class VocalFeedback {
        private final String fillerWords;
        private final Integer fillerWordCount;
        private final String speechPace;
        private final String toneConfidenceLevel;
        private final String emotionLabel;
        private final CommentBlock vocalComment;
    }

    @Getter
    @Builder
    public static class TechnicalFeedback {
        private final String rubricCategory;
        private final String rubricId;
        private final String levelFlag;
        private final List<TechnicalDimensionFeedback> dimensions;
    }

    @Getter
    @Builder
    public static class TechnicalDimensionFeedback {
        private final String dimension;
        private final Integer score;
        private final String observation;
        private final String evidenceQuote;
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback) {
        return from(feedback, null, List.of());
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback,
                                                  QuestionScore questionScore,
                                                  List<QuestionScoreDimension> dimensions) {
        Question question = feedback.getQuestion();

        NonverbalFeedback nonverbal = NonverbalFeedback.builder()
                .eyeContactLevel(feedback.getEyeContactLevel())
                .postureLevel(feedback.getPostureLevel())
                .expressionLabel(feedback.getExpressionLabel())
                .nonverbalComment(parseCommentBlock(feedback.getNonverbalComment()))
                .build();

        VocalFeedback vocal = VocalFeedback.builder()
                .fillerWords(feedback.getFillerWords())
                .fillerWordCount(feedback.getFillerWordCount())
                .speechPace(feedback.getSpeechPace())
                .toneConfidenceLevel(feedback.getToneConfidenceLevel())
                .emotionLabel(feedback.getEmotionLabel())
                .vocalComment(parseCommentBlock(feedback.getVocalComment()))
                .build();

        DeliveryFeedback delivery = DeliveryFeedback.builder()
                .nonverbal(nonverbal)
                .vocal(vocal)
                .attitudeComment(parseCommentBlock(feedback.getAttitudeComment()))
                .build();

        return TimestampFeedbackResponse.builder()
                .id(feedback.getId())
                .questionId(question != null ? question.getId() : null)
                .questionType(question != null ? question.getQuestionType().name() : null)
                .questionText(question != null ? question.getQuestionText() : null)
                .bestAnswer(question != null ? question.getBestAnswer() : null)
                .startMs(feedback.getStartMs())
                .endMs(feedback.getEndMs())
                .transcript(feedback.getTranscript())
                .delivery(delivery)
                .technicalFeedback(toTechnicalFeedback(question, questionScore, dimensions))
                .overallComment(parseCommentBlock(feedback.getOverallComment()))
                .isAnalyzed(feedback.isAnalyzed())
                .build();
    }

    static CommentBlock parseCommentBlock(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(json, CommentBlock.class);
        } catch (Exception e) {
            // legacy 또는 손상된 문자열 → positive에만 raw 입력
            return CommentBlock.builder().positive(json).build();
        }
    }

    private static TechnicalFeedback toTechnicalFeedback(Question question,
                                                          QuestionScore questionScore,
                                                          List<QuestionScoreDimension> dimensions) {
        if (questionScore == null || dimensions == null || dimensions.isEmpty()) {
            return null;
        }

        String perspective = (question != null && question.getQuestionType() != null)
                ? question.getQuestionType().rubricCategory().name()
                : null;

        List<TechnicalDimensionFeedback> dimFeedbacks = dimensions.stream()
                .sorted(Comparator.comparing(QuestionScoreDimension::getDimensionRef))
                .map(d -> TechnicalDimensionFeedback.builder()
                        .dimension(d.getDimensionRef())
                        .score(d.getScore())
                        .observation(d.getObservation())
                        .evidenceQuote(d.getEvidenceQuote())
                        .build())
                .toList();

        return TechnicalFeedback.builder()
                .rubricCategory(perspective)
                .rubricId(questionScore.getRubricId())
                .levelFlag(questionScore.getLevelFlag())
                .dimensions(dimFeedbacks)
                .build();
    }
}
