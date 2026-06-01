package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.feedback.session.models.service.SessionFeedbackSynthesizer;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.vo.GapItem;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import com.rehearse.api.domain.feedback.session.vo.StrengthItem;
import com.rehearse.api.domain.feedback.session.vo.WeekPlanItem;
import com.rehearse.api.infra.ai.adapter.ClaudeSessionFeedbackSynthesizer;
import com.rehearse.api.infra.ai.adapter.OpenAiSessionFeedbackSynthesizer;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnMissingBean({OpenAiSessionFeedbackSynthesizer.class, ClaudeSessionFeedbackSynthesizer.class})
public class MockSessionFeedbackSynthesizer implements SessionFeedbackSynthesizer {

    @PostConstruct
    void init() {
        log.warn("=== MockSessionFeedbackSynthesizer 활성화: API 키 없이 Mock 세션 피드백으로 동작합니다 ===");
    }

    @Override
    public GeneratedSessionFeedback synthesize(SessionFeedbackInput input) {
        OverallSection overall = new OverallSection(
                "Mock 레벨 평가 — API 키 미설정 환경 fallback",
                "Mock 세션 피드백 narrative",
                input.coverage() != null ? input.coverage() : "all turns scored"
        );
        List<StrengthItem> strengths = List.of(
                new StrengthItem("1-1 답변에서 요구사항 분해", "Mock 강점")
        );
        List<GapItem> gaps = List.of(
                new GapItem("2-1 답변에서 트레이드오프 비교 부족", "비교표 작성 연습")
        );
        List<WeekPlanItem> weekPlan = List.of(
                new WeekPlanItem(1, "Mock 학습 주제", List.of("Mock 자료"), "Mock 실습")
        );
        return new GeneratedSessionFeedback(overall, strengths, gaps, null, weekPlan);
    }
}
