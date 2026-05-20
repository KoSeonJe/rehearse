package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.models.service.FollowUpQuestionGenerator;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.adapter.ClaudeFollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.OpenAiFollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean({OpenAiFollowUpQuestionGenerator.class, ClaudeFollowUpQuestionGenerator.class})
public class MockFollowUpQuestionGenerator implements FollowUpQuestionGenerator {

    @PostConstruct
    void init() {
        log.warn("=== MockFollowUpQuestionGenerator 활성화: API 키 없이 Mock 후속 질문으로 동작합니다 ===");
    }

    @Override
    public GeneratedFollowUp generate(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis,
            ResumeSkeleton resumeSkeleton
    ) {
        return new GeneratedFollowUp(
                false,
                null,
                "방금 답변하신 내용 중 가장 자신 있는 부분을 한 가지만 더 자세히 설명해 주실 수 있을까요?",
                "방금 답변하신 내용 중 가장 자신 있는 부분을 한 가지만 더 자세히 설명해 주실 수 있을까요?",
                "Mock 후속 질문 — API 키 미설정 환경 fallback",
                "DEEP_DIVE",
                null,
                userAnswer,
                null
        );
    }
}
