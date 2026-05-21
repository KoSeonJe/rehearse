package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.adapter.ClaudeAnswerAnalyzer;
import com.rehearse.api.infra.ai.adapter.OpenAiAnswerAnalyzer;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnMissingBean({OpenAiAnswerAnalyzer.class, ClaudeAnswerAnalyzer.class})
public class MockAnswerAnalyzer implements AnswerAnalyzer {

    @PostConstruct
    void init() {
        log.warn("=== MockAnswerAnalyzer 활성화: API 키 없이 Mock 답변 분석으로 동작합니다 ===");
    }

    @Override
    public GeneratedAnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        return new GeneratedAnswerAnalysis(
                List.of(),
                Map.of("clarity", 1, "depth", 1, "evidence", 1),
                "depth",
                List.of(),
                RecommendedNextAction.SKIP
        );
    }
}
