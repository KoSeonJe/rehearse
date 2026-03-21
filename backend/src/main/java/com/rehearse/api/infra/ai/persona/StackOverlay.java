package com.rehearse.api.infra.ai.persona;

import java.util.Map;

/**
 * Overlay YAML에서 로드한 기술 스택별 전문 페르소나.
 * BaseProfile에 APPEND 또는 REPLACE 방식으로 병합된다.
 *
 * <ul>
 *   <li>fullPersona / mediumPersona / minimalPersona — 질문 생성 깊이별 페르소나</li>
 *   <li>interviewTypeGuideMap — 면접 유형별 가이드 (REPLACE)</li>
 *   <li>followUpDepthAppend — base follow_up_depth에 APPEND</li>
 *   <li>verbalExpertise — 언어 분석용 키워드 사전 (REPLACE)</li>
 * </ul>
 */
public record StackOverlay(
        String fullPersona,
        String mediumPersona,
        String minimalPersona,
        Map<String, String> interviewTypeGuideMap,
        String followUpDepthAppend,
        String verbalExpertise
) {}
