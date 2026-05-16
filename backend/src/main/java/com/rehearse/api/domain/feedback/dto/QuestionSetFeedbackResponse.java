package com.rehearse.api.domain.feedback.dto;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.question.entity.Question;
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
        return from(feedback, streamingUrl, fallbackUrl, Map.of(), Map.of());
    }

    public static QuestionSetFeedbackResponse from(QuestionSetFeedback feedback,
                                                    String streamingUrl, String fallbackUrl,
                                                    Map<Long, List<QuestionScore>> questionScoresByQuestionId,
                                                    Map<Long, List<QuestionScoreDimension>> dimensionsByQuestionScoreId) {
        List<TimestampFeedbackResponse> timestamps = feedback.getTimestampFeedbacks().stream()
                .map(timestamp -> {
                    List<QuestionScore> scores = resolveQuestionScores(timestamp, questionScoresByQuestionId);
                    return TimestampFeedbackResponse.from(timestamp, scores, dimensionsByQuestionScoreId);
                })
                .toList();

        return QuestionSetFeedbackResponse.builder()
                .id(feedback.getId())
                .questionSetComment(feedback.getQuestionSetComment())
                .streamingUrl(streamingUrl)
                .fallbackUrl(fallbackUrl)
                .timestampFeedbacks(timestamps)
                .build();
    }

    private static List<QuestionScore> resolveQuestionScores(TimestampFeedback timestamp,
                                                              Map<Long, List<QuestionScore>> questionScoresByQuestionId) {
        Question question = timestamp.getQuestion();
        if (question == null) {
            return List.of();
        }
        return questionScoresByQuestionId.getOrDefault(question.getId(), List.of());
    }
}
