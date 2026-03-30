package com.rehearse.api.domain.questionset.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import lombok.Builder;
import lombok.Getter;

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
    private final boolean isAnalyzed;

    @Getter
    @Builder
    public static class ContentFeedback {
        private final String verbalComment;
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
    }

    @Getter
    @Builder
    public static class NonverbalFeedback {
        private final String eyeContactLevel;   // GOOD / AVERAGE / NEEDS_IMPROVEMENT
        private final String postureLevel;
        private final String expressionLabel;
        private final String nonverbalComment;
    }

    @Getter
    @Builder
    public static class VocalFeedback {
        private final String fillerWords;           // JSON 배열 문자열
        private final Integer fillerWordCount;
        private final String speechPace;
        private final String toneConfidenceLevel;   // GOOD / AVERAGE / NEEDS_IMPROVEMENT
        private final String emotionLabel;
        private final String vocalComment;
    }

    public static TimestampFeedbackResponse from(TimestampFeedback feedback) {
        Question question = feedback.getQuestion();

        List<AccuracyIssue> accuracyIssues = parseAccuracyIssues(feedback.getAccuracyIssues());

        CoachingResponse coaching = CoachingResponse.builder()
                .structure(feedback.getCoachingStructure())
                .improvement(feedback.getCoachingImprovement())
                .build();

        ContentFeedback content = ContentFeedback.builder()
                .verbalComment(feedback.getVerbalComment())
                .accuracyIssues(accuracyIssues)
                .coaching(coaching)
                .build();

        NonverbalFeedback nonverbal = NonverbalFeedback.builder()
                .eyeContactLevel(feedback.getEyeContactLevel())
                .postureLevel(feedback.getPostureLevel())
                .expressionLabel(feedback.getExpressionLabel())
                .nonverbalComment(feedback.getNonverbalComment())
                .build();

        VocalFeedback vocal = VocalFeedback.builder()
                .fillerWords(feedback.getFillerWords())
                .fillerWordCount(feedback.getFillerWordCount())
                .speechPace(feedback.getSpeechPace())
                .toneConfidenceLevel(feedback.getToneConfidenceLevel())
                .emotionLabel(feedback.getEmotionLabel())
                .vocalComment(feedback.getVocalComment())
                .build();

        DeliveryFeedback delivery = DeliveryFeedback.builder()
                .nonverbal(nonverbal)
                .vocal(vocal)
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
                .isAnalyzed(feedback.isAnalyzed())
                .build();
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
