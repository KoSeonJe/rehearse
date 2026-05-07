package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;

public enum QuestionType {
    MAIN(null, null),
    FOLLOWUP(null, null),
    RESUME_OPENER(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_PLAYGROUND(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_INTERROGATION(ReferenceType.GUIDE, FeedbackPerspective.TECHNICAL),
    RESUME_WRAP_UP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL);

    private final ReferenceType referenceType;
    private final FeedbackPerspective feedbackPerspective;

    QuestionType(ReferenceType referenceType, FeedbackPerspective feedbackPerspective) {
        this.referenceType = referenceType;
        this.feedbackPerspective = feedbackPerspective;
    }

    public ReferenceType referenceType() {
        return referenceType;
    }

    public FeedbackPerspective feedbackPerspective() {
        return feedbackPerspective;
    }
}
