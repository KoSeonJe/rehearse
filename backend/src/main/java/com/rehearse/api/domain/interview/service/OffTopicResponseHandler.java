package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.global.config.IntentClassifierProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OffTopicResponseHandler implements IntentResponseHandler {

    public static final String OFF_TOPIC_CONNECTOR = OffTopicMarker.CONNECTOR;

    private static final List<String> LEAD_IN_POOL = List.of(
            "방금 답변은 질문 주제에서 벗어난 것 같습니다.",
            "응답이 질문 범위 밖으로 보입니다.",
            "지금 내용은 현재 질문과 직접 관련이 없습니다.",
            "질문과 다소 다른 방향의 답변으로 판단됩니다."
    );

    private final OffTopicEscalationDetector escalationDetector;
    private final GiveUpResponseHandler giveUpResponseHandler;
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
        return buildRedirectResponse(input);
    }

    private FollowUpResponse buildRedirectResponse(IntentBranchInput input) {
        int idx = Math.floorMod(
                Long.hashCode(input.interviewId() ^ ((long) input.turnIndex() * 31L)),
                LEAD_IN_POOL.size());
        String leadIn = LEAD_IN_POOL.get(idx);
        String combined = leadIn + " " + OffTopicMarker.CONNECTOR + " " + input.mainQuestion();

        return FollowUpResponse.intentBranch(new FollowUpResponse.IntentBranchPayload(
                combined,
                combined,
                "사용자 발화가 질문 범위 밖",
                OffTopicMarker.FOLLOW_UP_TYPE,
                OffTopicMarker.SKIP_REASON,
                input.answerText()
        ));
    }
}
