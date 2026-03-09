package com.devlens.api.domain.interview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUpResponse {

    private final String question;
    private final String reason;
    private final String type;
}
