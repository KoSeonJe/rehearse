package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateStatusResponse {

    private final Long id;
    private final InterviewStatus status;

    public static UpdateStatusResponse from(Interview interview) {
        return UpdateStatusResponse.builder()
                .id(interview.getId())
                .status(interview.getStatus())
                .build();
    }
}
