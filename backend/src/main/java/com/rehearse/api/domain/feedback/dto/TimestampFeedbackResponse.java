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
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Getter
@Builder
public class TimestampFeedbackResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NONVERBAL_RUBRIC_ID = "nonverbal-v1";
    private static final Set<String> VERBAL_RUBRIC_IDS = Set.of("resume-v1", "behavioral-v1", "technical-v1");

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
    private final NonverbalRubricFeedback nonverbalFeedback;
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

    public record NonverbalRubricFeedback(
            String rubricId,
            List<TechnicalDimensionFeedback> dimensions
    ) {
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback) {
        return from(feedback, List.of(), Map.of());
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback,
                                                  List<QuestionScore> questionScores,
                                                  Map<Long, List<QuestionScoreDimension>> dimsByScoreId) {
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

        TechnicalFeedback technicalFeedback = null;
        NonverbalRubricFeedback nonverbalFeedback = null;
        for (QuestionScore questionScore : questionScores) {
            String rubricId = questionScore.getRubricId();
            List<QuestionScoreDimension> dimensions = dimsByScoreId.getOrDefault(questionScore.getId(), List.of());
            if (NONVERBAL_RUBRIC_ID.equals(rubricId)) {
                nonverbalFeedback = toNonverbalRubricFeedback(questionScore, dimensions);
            } else if (VERBAL_RUBRIC_IDS.contains(rubricId)) {
                technicalFeedback = toTechnicalFeedback(question, questionScore, dimensions);
            } else {
                log.debug("legacy rubric_id 응답 미포함 questionScoreId={} rubricId={}",
                        questionScore.getId(), rubricId);
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
                .delivery(delivery)
                .technicalFeedback(technicalFeedback)
                .nonverbalFeedback(nonverbalFeedback)
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
                        .build())
                .toList();
    }
}
