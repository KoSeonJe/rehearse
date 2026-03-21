package com.rehearse.api.infra.ai.persona;

import java.util.Map;

public record ResolvedProfile(
        String fullPersona,
        String mediumPersona,
        String minimalPersona,
        String evaluationPerspective,
        Map<String, String> interviewTypeGuideMap,
        String followUpDepth,
        String verbalExpertise
) {
    public String getPersona(PersonaDepth depth) {
        return switch (depth) {
            case FULL    -> fullPersona;
            case MEDIUM  -> mediumPersona;
            case MINIMAL -> minimalPersona;
        };
    }

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
