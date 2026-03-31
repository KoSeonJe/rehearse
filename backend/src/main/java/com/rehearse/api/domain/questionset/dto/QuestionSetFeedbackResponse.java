package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.QuestionSetFeedback;
import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
        List<TimestampFeedbackResponse> timestamps = feedback.getTimestampFeedbacks().stream()
                .map(TimestampFeedbackResponse::from)
                .toList();

        return QuestionSetFeedbackResponse.builder()
                .id(feedback.getId())
                .questionSetComment(feedback.getQuestionSetComment())
                .streamingUrl(streamingUrl)
                .fallbackUrl(fallbackUrl)
                .timestampFeedbacks(timestamps)
                .build();
    }
}
