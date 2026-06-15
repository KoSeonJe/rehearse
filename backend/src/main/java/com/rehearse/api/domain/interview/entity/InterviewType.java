package com.rehearse.api.domain.interview.entity;

public enum InterviewType {
    // 공통
    CS_FUNDAMENTAL,
    BEHAVIORAL,
    RESUME_BASED,

    // 언어/프레임워크 심화
    LANGUAGE_FRAMEWORK,
    SYSTEM_DESIGN,

    // 풀스택 기술 심화
    FULLSTACK_STACK,

    // UI 프레임워크 심화
    UI_FRAMEWORK,
    BROWSER_PERFORMANCE,

    // 데브옵스 특화
    INFRA_CICD,
    CLOUD,

    // 데이터 특화
    DATA_PIPELINE,
    SQL_MODELING
}
