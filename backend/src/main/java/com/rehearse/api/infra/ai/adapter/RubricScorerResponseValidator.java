package com.rehearse.api.infra.ai.adapter;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RubricScorerResponseValidator {

    private static final Pattern KOREAN_SYLLABLE = Pattern.compile("[\\uAC00-\\uD7A3]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public ValidationResult validate(
            String dimensionKey,
            Integer score,
            String observation,
            String evidenceQuote,
            String userAnswer
    ) {
        if (score == null || score < 1 || score > 3) {
            return ValidationResult.violated(Violation.INVALID_SCORE, "score");
        }
        if (observation == null || observation.isBlank()) {
            return ValidationResult.violated(Violation.MISSING_OBSERVATION, "observation");
        }
        if (!KOREAN_SYLLABLE.matcher(observation).find()) {
            return ValidationResult.violated(Violation.NON_KOREAN_OBSERVATION, "observation");
        }
        if (evidenceQuote == null || evidenceQuote.isBlank()) {
            return ValidationResult.violated(Violation.MISSING_EVIDENCE, "evidence_quote");
        }
        String normalizedAnswer = normalize(userAnswer);
        String normalizedQuote = normalize(evidenceQuote);
        if (normalizedQuote.isEmpty() || !normalizedAnswer.contains(normalizedQuote)) {
            return ValidationResult.violated(Violation.EVIDENCE_NOT_IN_USER_ANSWER, "evidence_quote");
        }
        return ValidationResult.passed();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    public enum Violation {
        INVALID_SCORE,
        MISSING_OBSERVATION,
        NON_KOREAN_OBSERVATION,
        MISSING_EVIDENCE,
        EVIDENCE_NOT_IN_USER_ANSWER
    }

    public record ValidationResult(boolean valid, Violation violation, String field) {

        public static ValidationResult passed() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult violated(Violation violation, String field) {
            return new ValidationResult(false, violation, field);
        }
    }
}
