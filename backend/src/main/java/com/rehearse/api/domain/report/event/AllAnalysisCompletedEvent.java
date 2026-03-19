package com.rehearse.api.domain.report.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AllAnalysisCompletedEvent {

    private final Long interviewId;
}
