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
    private final int questionSetScore;
    private final String questionSetComment;
    private final String streamingUrl;
    private final String fallbackUrl;
    private final List<TimestampFeedbackResponse> timestampFeedbacks;
    private final String verbalSummary;
    private final String vocalSummary;
    private final String nonverbalSummary;
    private final String strengths;          // JSON 배열 문자열
    private final String improvements;       // JSON 배열 문자열
    private final String topPriorityAdvice;

    public static QuestionSetFeedbackResponse from(QuestionSetFeedback feedback,
                                                     String streamingUrl, String fallbackUrl) {
        List<TimestampFeedbackResponse> timestamps = feedback.getTimestampFeedbacks().stream()
                .map(TimestampFeedbackResponse::from)
                .toList();

        return QuestionSetFeedbackResponse.builder()
                .id(feedback.getId())
                .questionSetScore(feedback.getQuestionSetScore())
                .questionSetComment(feedback.getQuestionSetComment())
                .streamingUrl(streamingUrl)
                .fallbackUrl(fallbackUrl)
                .timestampFeedbacks(timestamps)
                .verbalSummary(feedback.getVerbalSummary())
                .vocalSummary(feedback.getVocalSummary())
                .nonverbalSummary(feedback.getNonverbalSummary())
                .strengths(feedback.getStrengths())
                .improvements(feedback.getImprovements())
                .topPriorityAdvice(feedback.getTopPriorityAdvice())
                .build();
    }
}
