package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;

public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, FeedbackPerspective.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, FeedbackPerspective.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, FeedbackPerspective.BEHAVIORAL),
    RESUME_OPENER(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_PLAYGROUND(ReferenceType.GUIDE, FeedbackPerspective.EXPERIENCE),
    RESUME_INTERROGATION(ReferenceType.GUIDE, FeedbackPerspective.TECHNICAL),
    MAIN(null, null),
    FOLLOWUP(null, null);

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

    public boolean isFollowUp() {
        return this == TECH_FOLLOWUP || this == BEHAVIORAL_FOLLOWUP || this == FOLLOWUP;
    }

    public boolean isMain() {
        return this == TECH_MAIN || this == BEHAVIORAL_MAIN || this == MAIN;
    }

    public boolean isResume() {
        return this == RESUME_OPENER || this == RESUME_PLAYGROUND
                || this == RESUME_INTERROGATION;
    }

    public ReferenceType referenceTypeOrFallback(QuestionSetCategory category) {
        if (referenceType != null) {
            return referenceType;
        }
        return category == QuestionSetCategory.BEHAVIORAL
                ? ReferenceType.GUIDE
                : ReferenceType.MODEL_ANSWER;
    }

    public FeedbackPerspective feedbackPerspectiveOrFallback(QuestionSetCategory category) {
        if (feedbackPerspective != null) {
            return feedbackPerspective;
        }
        return category == QuestionSetCategory.BEHAVIORAL
                ? FeedbackPerspective.BEHAVIORAL
                : FeedbackPerspective.TECHNICAL;
    }
}
