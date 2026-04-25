package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.vo.IntentType;

public interface IntentResponseHandler {

    IntentType supports();

    FollowUpResponse handle(IntentBranchInput input);
}
