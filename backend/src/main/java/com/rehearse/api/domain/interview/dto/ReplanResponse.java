package com.rehearse.api.domain.interview.dto;

public record ReplanResponse(Long interviewId, Long planId, boolean replaced) {

    public static ReplanResponse of(Long interviewId, Long planId, boolean replaced) {
        return new ReplanResponse(interviewId, planId, replaced);
    }
}
