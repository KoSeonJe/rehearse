package com.rehearse.api.domain.feedback.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FeedbackListResponse {

    private final Long interviewId;
    private final List<FeedbackResponse> feedbacks;
    private final int totalCount;
}
