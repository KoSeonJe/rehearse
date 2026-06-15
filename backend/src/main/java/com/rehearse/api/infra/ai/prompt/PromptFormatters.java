package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.question.entity.ReferenceType;

public final class PromptFormatters {

    private PromptFormatters() {}

    public static String toReferenceLabel(ReferenceType refType) {
        if (refType == null) {
            return "MODEL_ANSWER";
        }
        return refType.name();
    }
}
