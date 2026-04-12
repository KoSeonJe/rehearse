package com.rehearse.api.domain.reviewbookmark.dto;

import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;

import java.time.LocalDateTime;

/**
 * 복습 북마크 목록 항목의 DTO.
 *
 * <p>BE는 도메인 원시값만 전달한다. 포지션 라벨링, 제목 조합 등
 * 프레젠테이션 로직은 FE가 담당한다 — BE DTO에 사용자용 표시 문자열을
 * 미리 조합해 두면 FE 라벨 매핑을 우회하게 되어 관심사가 섞인다.
 */
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
        var qsf = tsf.getQuestionSetFeedback();
        var questionSet = qsf.getQuestionSet();
        var interview = questionSet.getInterview();
        Question question = tsf.getQuestion();

        return new ReviewBookmarkListItem(
                bookmark.getId(),
                tsf.getId(),
                question != null ? question.getQuestionText() : null,
                question != null ? question.getModelAnswer() : null,
                tsf.getTranscript(),
                tsf.getCoachingImprovement(),
                questionSet.getCategory(),
                interview.getPosition().name(),
                interview.getPositionDetail(),
                interview.getCreatedAt(),
                bookmark.getCreatedAt(),
                bookmark.getResolvedAt()
        );
    }
}
