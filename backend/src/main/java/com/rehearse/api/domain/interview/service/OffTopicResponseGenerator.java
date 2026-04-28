package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.OffTopicMarkers;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OffTopicResponseGenerator {

    private static final List<String> LEAD_IN_POOL = List.of(
            "방금 답변은 질문 주제에서 벗어난 것 같습니다.",
            "응답이 질문 범위 밖으로 보입니다.",
            "지금 내용은 현재 질문과 직접 관련이 없습니다.",
            "질문과 다소 다른 방향의 답변으로 판단됩니다."
    );

    public FollowUpResponse generate(IntentBranchInput input) {
        int idx = Math.floorMod(
                Long.hashCode(input.interviewId() ^ ((long) input.turnIndex() * 31L)),
                LEAD_IN_POOL.size());
        String leadIn = LEAD_IN_POOL.get(idx);
        String combined = leadIn + " " + OffTopicMarkers.CONNECTOR + " " + input.mainQuestion();

        return FollowUpResponse.intentBranch(new FollowUpResponse.IntentBranchPayload(
                combined,
                combined,
                "사용자 발화가 질문 범위 밖",
                OffTopicMarkers.FOLLOW_UP_TYPE,
                OffTopicMarkers.SKIP_REASON,
                input.answerText()
        ));
    }
}
