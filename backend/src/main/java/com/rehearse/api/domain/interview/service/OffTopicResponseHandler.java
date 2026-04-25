package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class OffTopicResponseHandler {

    public static final String OFF_TOPIC_CONNECTOR = "질문에 대한 답변을 적절히 해주세요.";

    private static final List<String> LEAD_IN_POOL = List.of(
            "방금 답변은 질문 주제에서 벗어난 것 같습니다.",
            "응답이 질문 범위 밖으로 보입니다.",
            "지금 내용은 현재 질문과 직접 관련이 없습니다.",
            "질문과 다소 다른 방향의 답변으로 판단됩니다."
    );

    public FollowUpResponse handle(Long interviewId, int turnIndex, String mainQuestion, String answerText) {
        int idx = Math.floorMod(Objects.hash(interviewId, turnIndex), LEAD_IN_POOL.size());
        String leadIn = LEAD_IN_POOL.get(idx);
        String combined = leadIn + " " + OFF_TOPIC_CONNECTOR + " " + mainQuestion;

        return FollowUpResponse.builder()
                .question(combined)
                .ttsQuestion(combined)
                .type("OFF_TOPIC_REDIRECT")
                .reason("사용자 발화가 질문 범위 밖")
                .answerText(answerText)
                .skip(true)
                .skipReason("OFF_TOPIC")
                .presentToUser(true)
                .build();
    }
}
