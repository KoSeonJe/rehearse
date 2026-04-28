package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.IntentType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntentDispatcher {

    private final List<IntentResponseHandler> handlers;
    private final Map<IntentType, IntentResponseHandler> handlerByIntent = new EnumMap<>(IntentType.class);

    @PostConstruct
    void register() {
        handlers.forEach(h -> handlerByIntent.put(h.supports(), h));
    }

    public FollowUpResponse dispatch(IntentType intentType, IntentBranchInput input) {
        IntentResponseHandler handler = handlerByIntent.get(intentType);
        if (handler == null) {
            // 모든 IntentType 에 대해 빈 매핑이 보장돼야 한다 — 빠진 핸들러는 빌드/스타트업 단계에서 실패시키자.
            throw new IllegalStateException("등록된 IntentResponseHandler 없음: intent=" + intentType);
        }
        return handler.handle(input);
    }
}
