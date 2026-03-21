package com.rehearse.api.infra.ai.persona;

import java.util.Map;

/**
 * BaseProfile + StackOverlay를 병합한 최종 페르소나 프로필.
 *
 * <p>병합 규칙:
 * <ul>
 *   <li>fullPersona        — base.personaBlock + overlay.fullPersona (APPEND)</li>
 *   <li>mediumPersona      — overlay.mediumPersona (REPLACE)</li>
 *   <li>minimalPersona     — overlay.minimalPersona (REPLACE)</li>
 *   <li>evaluationPerspective — base.evaluationPerspective (KEEP)</li>
 *   <li>interviewTypeGuideMap — overlay (REPLACE)</li>
 *   <li>followUpDepth      — base + overlay.followUpDepthAppend (APPEND)</li>
 *   <li>verbalExpertise    — overlay (REPLACE)</li>
 * </ul>
 */
public record ResolvedProfile(
        String fullPersona,
        String mediumPersona,
        String minimalPersona,
        String evaluationPerspective,
        Map<String, String> interviewTypeGuideMap,
        String followUpDepth,
        String verbalExpertise
) {
    /**
     * 요청된 깊이에 맞는 페르소나 문자열을 반환한다.
     */
    public String getPersona(PersonaDepth depth) {
        return switch (depth) {
            case FULL    -> fullPersona;
            case MEDIUM  -> mediumPersona;
            case MINIMAL -> minimalPersona;
        };
    }

    /**
     * Overlay가 없을 때 BaseProfile만으로 프로필을 생성하는 팩토리 메서드.
     * medium/minimal 페르소나는 base personaBlock의 첫 문장을 축약하여 생성한다.
     */
    public static ResolvedProfile fromBaseOnly(BaseProfile base) {
        String firstSentence = base.personaBlock().split("\n")[0];
        String minimalPersona = firstSentence.length() > 50
                ? firstSentence.substring(0, 50) + " 면접 답변을 분석합니다."
                : firstSentence + " 면접 답변을 분석합니다.";

        return new ResolvedProfile(
                base.personaBlock(),
                firstSentence,
                minimalPersona,
                base.evaluationPerspective(),
                Map.of(),
                base.followUpDepth(),
                ""
        );
    }
}
