package com.rehearse.api.domain.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.rubric.RubricIds;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.question.entity.Question;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TimestampFeedbackResponse {

    private final Long id;
    private final Long questionId;
    private final String questionType;
    private final String questionText;
    private final String bestAnswer;
    private final long startMs;
    private final long endMs;
    private final String transcript;
    private final String fillerWords;
    private final Integer fillerWordCount;
    private final TechnicalFeedback technicalFeedback;
    private final NonverbalRubricFeedback nonverbalFeedback;
    @JsonProperty("isAnalyzed")
    private final boolean isAnalyzed;

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
        private final String status;
    }

    public record NonverbalRubricFeedback(
            String rubricId,
            List<TechnicalDimensionFeedback> dimensions
    ) {
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback,
                                                  List<QuestionScore> questionScores,
                                                  Map<Long, List<QuestionScoreDimension>> dimsByScoreId) {
        Question question = feedback.getQuestion();

        TechnicalFeedback technicalFeedback = null;
        NonverbalRubricFeedback nonverbalFeedback = null;
        for (QuestionScore questionScore : questionScores) {
            String rubricId = questionScore.getRubricId();
            List<QuestionScoreDimension> dimensions = dimsByScoreId.getOrDefault(questionScore.getId(), List.of());
            if (RubricIds.NONVERBAL.equals(rubricId)) {
                nonverbalFeedback = toNonverbalRubricFeedback(questionScore, dimensions);
            } else {
                technicalFeedback = toTechnicalFeedback(question, questionScore, dimensions);
            }
        }

        return TimestampFeedbackResponse.builder()
                .id(feedback.getId())
                .questionId(question != null ? question.getId() : null)
                .questionType(question != null ? question.getQuestionType().name() : null)
                .questionText(question != null ? question.getQuestionText() : null)
                .bestAnswer(question != null ? question.getBestAnswer() : null)
                .startMs(feedback.getStartMs())
                .endMs(feedback.getEndMs())
                .transcript(feedback.getTranscript())
                .fillerWords(feedback.getFillerWords())
                .fillerWordCount(feedback.getFillerWordCount())
                .technicalFeedback(technicalFeedback)
                .nonverbalFeedback(nonverbalFeedback)
                .isAnalyzed(feedback.isAnalyzed())
                .build();
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

        return TechnicalFeedback.builder()
                .rubricCategory(perspective)
                .rubricId(questionScore.getRubricId())
                .levelFlag(questionScore.getLevelFlag())
                .dimensions(toDimensionFeedbacks(dimensions))
                .build();
    }

    private static NonverbalRubricFeedback toNonverbalRubricFeedback(QuestionScore questionScore,
                                                                       List<QuestionScoreDimension> dimensions) {
        if (questionScore == null || dimensions == null || dimensions.isEmpty()) {
            return null;
        }
        return new NonverbalRubricFeedback(questionScore.getRubricId(), toDimensionFeedbacks(dimensions));
    }

    private static List<TechnicalDimensionFeedback> toDimensionFeedbacks(List<QuestionScoreDimension> dimensions) {
        return dimensions.stream()
                .sorted(Comparator.comparing(QuestionScoreDimension::getDimensionRef))
                .map(d -> TechnicalDimensionFeedback.builder()
                        .dimension(d.getDimensionRef())
                        .score(d.getScore())
                        .observation(d.getObservation())
                        .evidenceQuote(d.getEvidenceQuote())
                        .status(d.getStatus() != null ? d.getStatus().name() : null)
                        .build())
                .toList();
    }
}
