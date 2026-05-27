package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory;

public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    RESUME_OPENER(ReferenceType.GUIDE, RubricCategory.EXPERIENCE),
    RESUME_MAIN(ReferenceType.GUIDE, RubricCategory.TECHNICAL),
    RESUME_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.TECHNICAL);

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
        return this == TECH_FOLLOWUP || this == BEHAVIORAL_FOLLOWUP || this == RESUME_FOLLOWUP;
    }

    public boolean isMain() {
        return this == TECH_MAIN || this == BEHAVIORAL_MAIN || this == RESUME_MAIN;
    }

    public boolean isResume() {
        return this == RESUME_OPENER || this == RESUME_MAIN || this == RESUME_FOLLOWUP;
    }

    public QuestionCategory category() {
        return switch (this) {
            case TECH_MAIN, TECH_FOLLOWUP -> QuestionCategory.CONCEPT;
            case BEHAVIORAL_MAIN, BEHAVIORAL_FOLLOWUP -> QuestionCategory.EXPERIENCE;
            case RESUME_OPENER, RESUME_MAIN, RESUME_FOLLOWUP -> QuestionCategory.RESUME;
        };
    }
}
