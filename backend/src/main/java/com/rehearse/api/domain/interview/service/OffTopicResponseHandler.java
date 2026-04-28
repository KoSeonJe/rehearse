package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.interview.entity.OffTopicMarkers;
import com.rehearse.api.global.config.IntentClassifierProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OffTopicResponseHandler implements IntentResponseHandler {

    public static final String OFF_TOPIC_CONNECTOR = OffTopicMarkers.CONNECTOR;

    private final OffTopicEscalationDetector escalationDetector;
    private final GiveUpResponseHandler giveUpResponseHandler;
    private final OffTopicResponseGenerator offTopicResponseGenerator;
    private final IntentClassifierProperties properties;

    @Override
    public IntentType supports() {
        return IntentType.OFF_TOPIC;
    }

    @Override
    public FollowUpResponse handle(IntentBranchInput input) {
        int consecutive = escalationDetector.countRecentConsecutive(input.previousExchanges());
        if (escalationDetector.shouldEscalate(consecutive, properties.offTopicConsecutiveLimit())) {
            log.info("OFF_TOPIC 연속 {}회 → GIVE_UP escalation", consecutive + 1);
            return giveUpResponseHandler.handle(input);
        }
        return offTopicResponseGenerator.generate(input);
    }
}
