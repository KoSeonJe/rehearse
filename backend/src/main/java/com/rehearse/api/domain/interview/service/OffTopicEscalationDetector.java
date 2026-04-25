package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OffTopicEscalationDetector {

    public int countRecentConsecutive(List<FollowUpExchange> previousExchanges) {
        if (previousExchanges == null || previousExchanges.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = previousExchanges.size() - 1; i >= 0; i--) {
            FollowUpExchange ex = previousExchanges.get(i);
            if (ex.getQuestion() != null
                    && ex.getQuestion().contains(OffTopicResponseHandler.OFF_TOPIC_CONNECTOR)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public boolean shouldEscalate(int consecutive, int limit) {
        return consecutive + 1 >= limit;
    }
}
