package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.AnswerFeedbackPerspective;
import com.rehearse.api.domain.question.entity.ReferenceType;

import java.util.List;
import java.util.stream.Collectors;

public final class PromptFormatters {

    private PromptFormatters() {}

    public static String formatPerspectives(List<AnswerFeedbackPerspective> perspectives) {
        if (perspectives == null || perspectives.isEmpty()) {
            return "(없음)";
        }
        return perspectives.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    public static String toReferenceLabel(ReferenceType refType) {
        if (refType == null) {
            return "CONCEPT";
        }
        return switch (refType) {
            case GUIDE -> "EXPERIENCE";
            case MODEL_ANSWER -> "CONCEPT";
        };
    }
}
