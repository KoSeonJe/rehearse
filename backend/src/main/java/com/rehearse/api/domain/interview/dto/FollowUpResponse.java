package com.rehearse.api.domain.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUpResponse {

    private final Long questionId;
    private final String question;
    private final String reason;
    private final String type;
}
