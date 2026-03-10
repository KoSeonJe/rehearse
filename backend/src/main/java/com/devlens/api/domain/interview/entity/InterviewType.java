package com.devlens.api.domain.interview.entity;

public enum InterviewType {
    // 공통
    CS_FUNDAMENTAL,
    BEHAVIORAL,
    RESUME_BASED,

    // 백엔드 특화
    JAVA_SPRING,
    SYSTEM_DESIGN,

    // 풀스택 특화
    FULLSTACK_JS,

    // 프론트엔드 특화
    REACT_COMPONENT,
    BROWSER_PERFORMANCE,

    // 데브옵스 특화
    INFRA_CICD,
    CLOUD,

    // 데이터 특화
    DATA_PIPELINE,
    SQL_MODELING
}
