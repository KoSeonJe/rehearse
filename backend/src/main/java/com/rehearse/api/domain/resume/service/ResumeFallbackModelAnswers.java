package com.rehearse.api.domain.resume.service;

final class ResumeFallbackModelAnswers {

    static final String OPENER =
            "프로젝트의 배경, 본인 역할, 가장 인상 깊었던 기술적/비기술적 경험을 STAR 구조로 1~2문단 정리하세요.";
    static final String PLAYGROUND =
            "방금 답변 중 가장 결정적이었던 선택과 그 근거를 짧은 일화로 풀어내고, 결과·배운 점까지 한 줄로 잇는 게 좋습니다.";
    static final String INTERROGATION =
            "현재 단계 (WHAT/HOW/WHY/TRADEOFF) 에 맞춰 핵심 개념·구현 방식·선택 근거를 구분해 답하고, 마지막에 트레이드오프를 한 줄 덧붙이세요.";
    static final String WRAP_UP =
            "이번 세션에서 가장 인상 깊었던 깨달음, 다음에 보강하고 싶은 주제, 후속 학습 계획을 짧게 정리하고 마지막 한 마디로 마무리하세요.";

    private ResumeFallbackModelAnswers() {}
}
