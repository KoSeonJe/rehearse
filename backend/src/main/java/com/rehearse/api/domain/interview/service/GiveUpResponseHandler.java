package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.IntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GiveUpResponseHandler implements IntentResponseHandler {

    private final GiveUpResponseGenerator generator;

    @Override
    public IntentType supports() {
        return IntentType.GIVE_UP;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        return generator.generate(input);
    }
}
