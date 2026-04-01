package com.rehearse.api.domain.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewStatsResponse {

    private final long totalCount;
    private final long completedCount;
    private final long thisWeekCount;
}
