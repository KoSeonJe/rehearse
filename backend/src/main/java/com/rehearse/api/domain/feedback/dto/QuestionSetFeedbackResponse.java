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
                                                    Map<Long, QuestionScore> questionScoreByQuestionId,
                                                    Map<Long, List<QuestionScoreDimension>> dimensionsByQuestionScoreId) {
        List<TimestampFeedbackResponse> timestamps = feedback.getTimestampFeedbacks().stream()
                .map(timestamp -> {
                    QuestionScore qs = resolveQuestionScore(timestamp, questionScoreByQuestionId);
                    List<QuestionScoreDimension> dims = qs != null
                            ? dimensionsByQuestionScoreId.getOrDefault(qs.getId(), List.of())
                            : List.of();
                    return TimestampFeedbackResponse.from(timestamp, qs, dims);
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

    private static QuestionScore resolveQuestionScore(TimestampFeedback timestamp,
                                                       Map<Long, QuestionScore> questionScoreByQuestionId) {
        Question question = timestamp.getQuestion();
        if (question == null) {
            return null;
        }
        return questionScoreByQuestionId.get(question.getId());
    }
}
