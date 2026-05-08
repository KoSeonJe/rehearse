package com.rehearse.api.global.support;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;

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
                null, null, null);
    }

    public static QuestionPool createQuestionPool(String cacheKey, String content) {
        return QuestionPool.create(cacheKey, content, null, null, null);
    }

    public static QuestionPool createQuestionPool(String cacheKey, String content,
                                                  String ttsContent, String category) {
        return QuestionPool.create(cacheKey, content, ttsContent, category, null);
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

    // ── Question ──

    public static Question createResumeQuestion(QuestionType questionType) {
        return Question.builder()
                .questionType(questionType)
                .questionText("이력서 기반 테스트 질문")
                .orderIndex(0)
                .build();
    }

    // ── TimestampFeedback ──

    public static TimestampFeedback createTimestampFeedback(Question question) {
        return TimestampFeedback.builder()
                .question(question)
                .startMs(0)
                .endMs(1000)
                .transcript("테스트 답변")
                .isAnalyzed(true)
                .build();
    }

    // ── QuestionScore ──

    public static QuestionScore createQuestionScore(Long questionId, String rubricId, String levelFlag) {
        return QuestionScore.builder()
                .questionId(questionId)
                .interviewId(1L)
                .rubricId(rubricId)
                .levelFlag(levelFlag)
                .build();
    }

    public static QuestionScoreDimension createQuestionScoreDimension(Long questionScoreId,
                                                                      String dimensionRef,
                                                                      Integer score,
                                                                      String observation) {
        return QuestionScoreDimension.builder()
                .questionScoreId(questionScoreId)
                .dimensionRef(dimensionRef)
                .score(score)
                .observation(observation)
                .evidenceQuote(null)
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
