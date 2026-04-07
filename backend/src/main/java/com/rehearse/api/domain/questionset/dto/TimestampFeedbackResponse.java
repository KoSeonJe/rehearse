package com.rehearse.api.domain.questionset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
public class TimestampFeedbackResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Long id;
    private final Long questionId;
    private final String questionType;
    private final String questionText;
    private final String modelAnswer;
    private final long startMs;
    private final long endMs;
    private final String transcript;
    private final ContentFeedback content;
    private final DeliveryFeedback delivery;
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
    public static class ContentFeedback {
        private final CommentBlock verbalComment;
        private final List<AccuracyIssue> accuracyIssues;
        private final CoachingResponse coaching;
    }

    @Getter
    @Builder
    public static class AccuracyIssue {
        private final String claim;
        private final String correction;
    }

    @Getter
    @Builder
    public static class CoachingResponse {
        private final String structure;
        private final String improvement;
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
        private final String eyeContactLevel;   // GOOD / AVERAGE / NEEDS_IMPROVEMENT
        private final String postureLevel;
        private final String expressionLabel;
        private final CommentBlock nonverbalComment;
    }

    @Getter
    @Builder
    public static class VocalFeedback {
        private final String fillerWords;           // JSON 배열 문자열
        private final Integer fillerWordCount;
        private final String speechPace;
        private final String toneConfidenceLevel;   // GOOD / AVERAGE / NEEDS_IMPROVEMENT
        private final String emotionLabel;
        private final CommentBlock vocalComment;
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback) {
        Question question = feedback.getQuestion();

        List<AccuracyIssue> accuracyIssues = parseAccuracyIssues(feedback.getAccuracyIssues());

        CoachingResponse coaching = CoachingResponse.builder()
                .structure(feedback.getCoachingStructure())
                .improvement(feedback.getCoachingImprovement())
                .build();

        ContentFeedback content = ContentFeedback.builder()
                .verbalComment(parseCommentBlock(feedback.getVerbalComment()))
                .accuracyIssues(accuracyIssues)
                .coaching(coaching)
                .build();

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
                .modelAnswer(question != null ? question.getModelAnswer() : null)
                .startMs(feedback.getStartMs())
                .endMs(feedback.getEndMs())
                .transcript(feedback.getTranscript())
                .content(content)
                .delivery(delivery)
                .overallComment(parseCommentBlock(feedback.getOverallComment()))
                .isAnalyzed(feedback.isAnalyzed())
                .build();
    }

    static CommentBlock parseCommentBlock(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(json, CommentBlock.class);
        } catch (Exception e) {
            // legacy ✓△→ 또는 손상된 문자열 → positive에만 raw 입력
            return CommentBlock.builder().positive(json).build();
        }
    }

    private static List<AccuracyIssue> parseAccuracyIssues(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
