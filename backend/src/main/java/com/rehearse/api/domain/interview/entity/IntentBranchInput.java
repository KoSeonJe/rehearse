package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;

import java.util.List;

public record IntentBranchInput(
        Long interviewId,
        FollowUpContext context,
        String mainQuestion,
        String answerText,
        int turnIndex,
        List<FollowUpExchange> previousExchanges
) {}
