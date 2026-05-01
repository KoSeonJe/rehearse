package com.rehearse.api.domain.feedback.dto;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class QuestionSetFeedbackResponse {

    private final Long id;
    private final String questionSetComment;
    private final String streamingUrl;
    private final String fallbackUrl;
    private final List<TimestampFeedbackResponse> timestampFeedbacks;

    public static QuestionSetFeedbackResponse from(QuestionSetFeedback feedback,
                                                     String streamingUrl, String fallbackUrl) {
        return from(feedback, streamingUrl, fallbackUrl, Map.of());
    }

    public static QuestionSetFeedbackResponse from(QuestionSetFeedback feedback,
                                                     String streamingUrl, String fallbackUrl,
                                                     Map<Long, RubricScore> rubricScoreByTurnId) {
        return from(feedback, streamingUrl, fallbackUrl, rubricScoreByTurnId, Map.of());
    }

    public static QuestionSetFeedbackResponse from(QuestionSetFeedback feedback,
                                                     String streamingUrl, String fallbackUrl,
                                                     Map<Long, RubricScore> rubricScoreByTurnId,
                                                     Map<String, String> dimensionLabels) {
        List<TimestampFeedbackResponse> timestamps = feedback.getTimestampFeedbacks().stream()
                .map(timestamp -> TimestampFeedbackResponse.from(timestamp, resolveRubricScore(timestamp, rubricScoreByTurnId), dimensionLabels))
                .toList();

        return QuestionSetFeedbackResponse.builder()
                .id(feedback.getId())
                .questionSetComment(feedback.getQuestionSetComment())
                .streamingUrl(streamingUrl)
                .fallbackUrl(fallbackUrl)
                .timestampFeedbacks(timestamps)
                .build();
    }

    private static RubricScore resolveRubricScore(TimestampFeedback timestamp, Map<Long, RubricScore> rubricScoreByTurnId) {
        if (timestamp.getQuestion() == null) {
            return null;
        }
        return rubricScoreByTurnId.get((long) timestamp.getQuestion().getOrderIndex());
    }
}
