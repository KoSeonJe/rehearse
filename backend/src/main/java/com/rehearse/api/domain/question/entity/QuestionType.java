package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory;

public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    RESUME_OPENER(ReferenceType.GUIDE, RubricCategory.EXPERIENCE),
    RESUME_PLAYGROUND(ReferenceType.GUIDE, RubricCategory.EXPERIENCE),
    RESUME_INTERROGATION(ReferenceType.GUIDE, RubricCategory.TECHNICAL);

    private final ReferenceType referenceType;
    private final RubricCategory rubricCategory;

    QuestionType(ReferenceType referenceType, RubricCategory rubricCategory) {
        this.referenceType = referenceType;
        this.rubricCategory = rubricCategory;
    }

    public ReferenceType referenceType() {
        return referenceType;
    }

    public RubricCategory rubricCategory() {
        return rubricCategory;
    }

    public boolean isFollowUp() {
        return this == TECH_FOLLOWUP || this == BEHAVIORAL_FOLLOWUP;
    }

    public boolean isMain() {
        return this == TECH_MAIN || this == BEHAVIORAL_MAIN;
    }

    public boolean isResume() {
        return this == RESUME_OPENER || this == RESUME_PLAYGROUND
                || this == RESUME_INTERROGATION;
    }
}
