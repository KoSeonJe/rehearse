package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.InterviewLevel;

import java.util.Map;

public class LevelGuideProvider {

    private static final Map<InterviewLevel, String> GUIDES = Map.of(
        InterviewLevel.JUNIOR,
            "JUNIOR: 기본 개념의 정확한 이해. 원리 중심 질문, CS 기초와 논리적 사고력 평가. 실무 경험보다 학습 의지.",
        InterviewLevel.MID,
            "MID: 실무 적용 경험과 문제 해결. 기술 선택 이유, 트레이드오프, 장애 시 판단력 평가.",
        InterviewLevel.SENIOR,
            "SENIOR: 아키텍처 의사결정, 팀 리딩, 기술 방향성. 시스템 조망, 조직 기술 영향력 평가."
    );

    public static String get(InterviewLevel level) {
        return GUIDES.get(level);
    }
}
