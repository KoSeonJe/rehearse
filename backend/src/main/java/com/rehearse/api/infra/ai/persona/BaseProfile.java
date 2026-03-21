package com.rehearse.api.infra.ai.persona;

/**
 * Base YAML에서 로드한 직군 공통 페르소나 프로필.
 * 기술 스택에 관계없이 공통 역량 평가 기준을 담는다.
 */
public record BaseProfile(
        String personaBlock,
        String evaluationPerspective,
        String followUpDepth
) {}
