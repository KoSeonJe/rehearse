package com.rehearse.api.domain.reviewbookmark.dto;

import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;

import java.time.LocalDateTime;

public record ReviewBookmarkListItem(
        Long id,
        Long timestampFeedbackId,
        String questionText,
        String modelAnswer,
        String transcript,
        String coachingImprovement,
        String interviewType,
        String interviewPosition,
        String interviewPositionDetail,
        LocalDateTime interviewDate,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public boolean isResolved() {
        return resolvedAt != null;
    }

    public static ReviewBookmarkListItem from(ReviewBookmark bookmark) {
        var tsf = bookmark.getTimestampFeedback();
        if (tsf == null) return null;

        var qsf = tsf.getQuestionSetFeedback();
        var questionSet = qsf != null ? qsf.getQuestionSet() : null;
        var interview = questionSet != null ? questionSet.getInterview() : null;
        Question question = tsf.getQuestion();

        return new ReviewBookmarkListItem(
                bookmark.getId(),
                tsf.getId(),
                question != null ? question.getQuestionText() : null,
                question != null ? question.getModelAnswer() : null,
                tsf.getTranscript(),
                tsf.getCoachingImprovement(),
                questionSet != null ? questionSet.getCategory().name() : null,
                interview != null ? interview.getPosition().name() : null,
                interview != null ? interview.getPositionDetail() : null,
                interview != null ? interview.getCreatedAt() : null,
                bookmark.getCreatedAt(),
                bookmark.getResolvedAt()
        );
    }
}
