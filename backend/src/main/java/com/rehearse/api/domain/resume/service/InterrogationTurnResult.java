package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;

public record InterrogationTurnResult(FollowUpResponse response, Long questionId) {}
