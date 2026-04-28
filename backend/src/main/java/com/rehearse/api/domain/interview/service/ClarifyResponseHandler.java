package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.IntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClarifyResponseHandler implements IntentResponseHandler {

    private final ClarifyResponseGenerator generator;

    @Override
    public IntentType supports() {
        return IntentType.CLARIFY_REQUEST;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        return generator.generate(input);
    }
}
