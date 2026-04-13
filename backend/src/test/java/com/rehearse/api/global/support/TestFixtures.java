package com.rehearse.api.global.support;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.generation.pool.entity.QuestionPool;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;

import java.util.List;

public final class TestFixtures {

    private TestFixtures() {
    }

    // ── Interview ──

    public static Interview createInterview() {
        return Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL))
                .durationMinutes(30)
                .techStack(TechStack.JAVA_SPRING)
                .build();
    }

    public static Interview createInterview(Long userId, Position position) {
        return Interview.builder()
                .userId(userId)
                .position(position)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .techStack(TechStack.getDefaultForPosition(position))
                .build();
    }

    public static Interview createInterview(Long userId, Position position, InterviewLevel level,
                                            List<InterviewType> interviewTypes) {
        return Interview.builder()
                .userId(userId)
                .position(position)
                .level(level)
                .interviewTypes(interviewTypes)
                .durationMinutes(30)
                .build();
    }

    // ── QuestionPool ──

    public static QuestionPool createQuestionPool() {
        return QuestionPool.create("backend:cs:junior", "Spring IoC 컨테이너에 대해 설명하세요.",
                null, null, null, null);
    }

    public static QuestionPool createQuestionPool(String cacheKey, String content) {
        return QuestionPool.create(cacheKey, content, null, null, null, null);
    }

    public static QuestionPool createQuestionPool(String cacheKey, String content,
                                                  String ttsContent, String category) {
        return QuestionPool.create(cacheKey, content, ttsContent, category, null, null);
    }

    // ── QuestionSet ──

    public static QuestionSet createQuestionSet(Interview interview) {
        return QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
    }

    public static QuestionSet createQuestionSet(Interview interview, QuestionSetCategory category,
                                                int orderIndex) {
        return QuestionSet.builder()
                .interview(interview)
                .category(category)
                .orderIndex(orderIndex)
                .build();
    }

    // ── ReviewBookmark ──

    public static ReviewBookmark createReviewBookmark(Long userId,
                                                      TimestampFeedback timestampFeedback) {
        return ReviewBookmark.builder()
                .userId(userId)
                .timestampFeedback(timestampFeedback)
                .build();
    }
}
