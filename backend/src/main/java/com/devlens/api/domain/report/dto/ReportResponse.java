package com.devlens.api.domain.report.dto;

import com.devlens.api.domain.report.entity.InterviewReport;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportResponse {

    private final Long id;
    private final Long interviewId;
    private final int overallScore;
    private final String summary;
    private final List<String> strengths;
    private final List<String> improvements;
    private final int feedbackCount;

    public static ReportResponse from(InterviewReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .interviewId(report.getInterview().getId())
                .overallScore(report.getOverallScore())
                .summary(report.getSummary())
                .strengths(report.getStrengths())
                .improvements(report.getImprovements())
                .feedbackCount(report.getFeedbackCount())
                .build();
    }
}
