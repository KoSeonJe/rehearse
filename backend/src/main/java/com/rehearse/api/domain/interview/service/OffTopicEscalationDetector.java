package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.OffTopicMarkers;
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
            if (isOffTopicTurn(previousExchanges.get(i))) {
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

    // 메타 필드(followUpType) 우선, 없으면 connector 텍스트 매칭으로 fallback
    private boolean isOffTopicTurn(FollowUpExchange exchange) {
        if (exchange == null) return false;
        if (OffTopicMarkers.FOLLOW_UP_TYPE.equals(exchange.getFollowUpType())) {
            return true;
        }
        return exchange.getQuestion() != null
                && exchange.getQuestion().contains(OffTopicMarkers.CONNECTOR);
    }
}
