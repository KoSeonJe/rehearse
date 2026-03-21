package com.rehearse.api.infra.ai.persona;

/**
 * 페르소나 적용 깊이.
 * 프롬프트 토큰 비용과 필요 정밀도에 따라 선택한다.
 *
 * <ul>
 *   <li>FULL    — 질문 생성용 (full persona + 모든 컨텍스트)</li>
 *   <li>MEDIUM  — 후속 질문용 (축약 페르소나)</li>
 *   <li>MINIMAL — 언어 분석용 (최소 페르소나)</li>
 * </ul>
 */
public enum PersonaDepth {
    FULL,
    MEDIUM,
    MINIMAL
}
